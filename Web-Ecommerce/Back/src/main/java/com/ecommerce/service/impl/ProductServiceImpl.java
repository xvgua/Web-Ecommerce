package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductForm;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.dto.SkuForm;
import com.ecommerce.entity.*;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.SearchLogService;
import com.ecommerce.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private static final Pattern BOOLEAN_SPECIAL = Pattern.compile("[+><()~*\"@\\\\-]");

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SearchLogService searchLogService;

    /**
     * Collect the given category ID plus all its descendant IDs.
     * Used so that filtering by a parent category also shows products
     * in its child categories.
     */
    private Set<Long> collectDescendantCategoryIds(Long categoryId) {
        Set<Long> result = new HashSet<>();
        result.add(categoryId);

        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Category>> parentToChildren = new HashMap<>();
        for (Category c : all) {
            if (c.getParentId() != null && c.getParentId() > 0) {
                parentToChildren.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }

        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            List<Category> children = parentToChildren.get(pid);
            if (children != null) {
                for (Category child : children) {
                    if (result.add(child.getId())) {
                        queue.add(child.getId());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public PageResult<Product> getProductPage(ProductQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            if ("exact".equals(query.getSearchMode())) {
                return exactSearch(query);
            }
            return keywordSearch(query);
        }
        return filterSearch(query);
    }

    /**
     * Keyword search: FULLTEXT (ngram) + LIKE + Levenshtein.
     * Always ordered by relevance (MATCH score, then LIKE match, then sales);
     * user-selected sort only applies to filterSearch (browsing without keyword).
     */
    private PageResult<Product> keywordSearch(ProductQuery query) {
        String keyword = query.getKeyword().trim();
        searchLogService.record(keyword, UserContext.getUserId());
        String escapedKeyword = escapeBooleanMode(keyword);
        String likeKeyword = escapeLikeWildcards(keyword);
        Integer status = query.getStatus() != null ? query.getStatus() : ProductStatus.ON_SALE;
        List<Long> categoryIds = query.getCategoryId() != null && query.getCategoryId() > 0
                ? new ArrayList<>(collectDescendantCategoryIds(query.getCategoryId())) : null;

        // Skip Levenshtein for very short keywords (≤2 chars):
        // edit distance ≤2 would match almost everything, making it noise
        String fuzzyKeyword = keyword.length() > 2 ? keyword.toLowerCase() : null;

        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        int offset = (page - 1) * pageSize;

        long total = productMapper.countByKeyword(escapedKeyword, likeKeyword, fuzzyKeyword, status, categoryIds);
        List<Product> records = productMapper.searchByKeyword(
                escapedKeyword, likeKeyword, fuzzyKeyword, status, categoryIds, offset, pageSize);

        fillCategoryNames(records);
        fillSkus(records);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * Exact match search: product name equals keyword exactly.
     */
    private PageResult<Product> exactSearch(ProductQuery query) {
        String keyword = query.getKeyword().trim();
        searchLogService.record(keyword, UserContext.getUserId());
        Integer status = query.getStatus() != null ? query.getStatus() : ProductStatus.ON_SALE;

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getName, keyword)
                .eq(Product::getStatus, status);
        if (query.getCategoryId() != null && query.getCategoryId() > 0) {
            Set<Long> categoryIds = collectDescendantCategoryIds(query.getCategoryId());
            wrapper.in(Product::getCategoryId, categoryIds);
        }

        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;

        Page<Product> result = productMapper.selectPage(new Page<>(page, pageSize), wrapper);

        fillCategoryNames(result.getRecords());
        fillSkus(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    /**
     * Standard filter search: category, price range, status (no keyword).
     */
    private PageResult<Product> filterSearch(ProductQuery query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        } else {
            wrapper.eq(Product::getStatus, ProductStatus.ON_SALE);
        }

        if (query.getCategoryId() != null && query.getCategoryId() > 0) {
            Set<Long> categoryIds = collectDescendantCategoryIds(query.getCategoryId());
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }

        applySort(wrapper, query.getSort());

        Page<Product> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Product> result = productMapper.selectPage(page, wrapper);

        fillCategoryNames(result.getRecords());
        fillSkus(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(),
                query.getPage(), query.getPageSize());
    }

    /**
     * Escape FULLTEXT BOOLEAN MODE reserved characters: + - > < ( ) ~ * " @
     */
    private String escapeBooleanMode(String keyword) {
        return BOOLEAN_SPECIAL.matcher(keyword).replaceAll("\\\\$0");
    }

    /**
     * Escape LIKE special chars: backslash (ESCAPE char), %, _.
     * Prevents user input like "50% off" from being treated as wildcards.
     */
    private String escapeLikeWildcards(String keyword) {
        return keyword.replace("\\", "\\\\")
                      .replace("%", "\\%")
                      .replace("_", "\\_");
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, String sort) {
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sales_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getSales);
        } else if ("rating_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getAvgRating);
        } else if ("rating_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getAvgRating);
        } else {
            wrapper.orderByDesc(Product::getListedAt);
        }
    }

    private void fillCategoryNames(List<Product> products) {
        if (products.isEmpty()) return;
        Set<Long> categoryIds = new HashSet<>();
        for (Product p : products) {
            if (p.getCategoryId() != null) categoryIds.add(p.getCategoryId());
        }
        if (categoryIds.isEmpty()) return;

        Map<Long, String> nameMap = new HashMap<>();
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        for (Category c : categories) {
            nameMap.put(c.getId(), c.getName());
        }
        for (Product p : products) {
            p.setCategoryName(nameMap.get(p.getCategoryId()));
        }
    }

    private void fillSkus(List<Product> products) {
        if (products.isEmpty()) return;
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toList());
        List<ProductSku> allSkus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().in(ProductSku::getProductId, productIds));
        Map<Long, List<ProductSku>> skuMap = new HashMap<>();
        for (ProductSku sku : allSkus) {
            skuMap.computeIfAbsent(sku.getProductId(), k -> new ArrayList<>()).add(sku);
        }
        for (Product p : products) {
            p.setSkus(skuMap.getOrDefault(p.getId(), java.util.Collections.emptyList()));
        }
    }

    @Override
    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (product.getCategoryId() != null) {
            Category c = categoryMapper.selectById(product.getCategoryId());
            if (c != null) product.setCategoryName(c.getName());
        }
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        product.setSkus(skus);
        return product;
    }

    @Override
    public List<Product> getHotProducts(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ON_SALE)
                .orderByDesc(Product::getSales)
                .last("LIMIT " + Math.min(limit, 50));
        return productMapper.selectList(wrapper);
    }

    @Override
    public List<Product> getNewProducts(int limit) {
        // This week's products first, fill to limit with most recent
        LocalDateTime weekStart = LocalDateTime.now().with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<Product> weekList = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatus.ON_SALE)
                        .isNotNull(Product::getListedAt)
                        .ge(Product::getListedAt, weekStart)
                        .orderByDesc(Product::getListedAt)
                        .last("LIMIT " + limit));
        if (weekList.size() >= limit) return weekList;

        // Not enough this week — fill with most recent from all time
        int remaining = limit - weekList.size();
        java.util.Set<Long> existingIds = weekList.stream().map(Product::getId).collect(java.util.stream.Collectors.toSet());
        List<Product> fillList = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatus.ON_SALE)
                        .isNotNull(Product::getListedAt)
                        .notIn(!existingIds.isEmpty(), Product::getId, existingIds)
                        .orderByDesc(Product::getListedAt)
                        .last("LIMIT " + remaining));
        weekList.addAll(fillList);
        return weekList;
    }

    @Override
    @Transactional
    public Product create(ProductForm form) {
        Product product = new Product();
        product.setName(form.getName());
        product.setCategoryId(form.getCategoryId());
        product.setPrice(form.getPrice());
        product.setStock(form.getStock() != null ? form.getStock() : 0);
        product.setDescription(form.getDescription());
        product.setDetail(form.getDetail());
        product.setMainImage(form.getMainImage());
        product.setImages(form.getImages());
        int status = form.getStatus() != null ? form.getStatus() : ProductStatus.ON_SALE;
        product.setStatus(status);
        product.setSales(0);
        if (status == ProductStatus.ON_SALE) {
            product.setListedAt(LocalDateTime.now());
        }
        productMapper.insert(product);

        if (form.getSkus() != null && !form.getSkus().isEmpty()) {
            for (SkuForm sf : form.getSkus()) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setSpecName(sf.getSpecName());
                sku.setSpecValue(sf.getSpecValue());
                sku.setPrice(sf.getPrice());
                sku.setStock(sf.getStock() != null ? sf.getStock() : 0);
                sku.setStatus(sf.getStatus() != null ? sf.getStatus() : 1);
                sku.setImage(sf.getImage());
                skuMapper.insert(sku);
            }
            syncProductFromSkus(product.getId());
            product = productMapper.selectById(product.getId());
        } else {
            createDefaultSku(product);
        }

        return product;
    }

    @Override
    @Transactional
    public void update(Long id, ProductForm form) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(404, "商品不存在");
        if (form.getName() != null) product.setName(form.getName());
        if (form.getCategoryId() != null) product.setCategoryId(form.getCategoryId());
        if (form.getPrice() != null) product.setPrice(form.getPrice());
        if (form.getStock() != null) product.setStock(form.getStock());
        if (form.getDescription() != null) product.setDescription(form.getDescription());
        if (form.getDetail() != null) product.setDetail(form.getDetail());
        if (form.getMainImage() != null) product.setMainImage(form.getMainImage());
        if (form.getImages() != null) product.setImages(form.getImages());
        if (form.getStatus() != null) {
            int oldStatus = product.getStatus() != null ? product.getStatus() : 0;
            product.setStatus(form.getStatus());
            if (form.getStatus() == ProductStatus.ON_SALE && oldStatus != ProductStatus.ON_SALE
                    && product.getListedAt() == null) {
                product.setListedAt(LocalDateTime.now());
            }
        }
        productMapper.updateById(product);

        if (form.getSkus() != null) {
            skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
            if (!form.getSkus().isEmpty()) {
                for (SkuForm sf : form.getSkus()) {
                    ProductSku sku = new ProductSku();
                    sku.setProductId(id);
                    sku.setSpecName(sf.getSpecName());
                    sku.setSpecValue(sf.getSpecValue());
                    sku.setPrice(sf.getPrice());
                    sku.setStock(sf.getStock() != null ? sf.getStock() : 0);
                    sku.setStatus(sf.getStatus() != null ? sf.getStatus() : 1);
                    sku.setImage(sf.getImage());
                    skuMapper.insert(sku);
                }
            } else {
                createDefaultSku(product);
            }
            syncProductFromSkus(id);
        } else {
            long count = skuMapper.selectCount(
                    new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
            if (count == 0) {
                createDefaultSku(product);
            }
        }
    }

    private void syncProductFromSkus(Long productId) {
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        if (skus.isEmpty()) return;

        BigDecimal minPrice = skus.stream()
                .map(ProductSku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        int totalStock = skus.stream()
                .mapToInt(s -> s.getStock() != null ? s.getStock() : 0)
                .sum();

        Product update = new Product();
        update.setId(productId);
        update.setPrice(minPrice);
        update.setStock(totalStock);
        productMapper.updateById(update);
    }

    private void createDefaultSku(Product product) {
        ProductSku sku = new ProductSku();
        sku.setProductId(product.getId());
        sku.setSpecName(product.getName());
        sku.setSpecValue("");
        sku.setPrice(product.getPrice());
        sku.setStock(product.getStock() != null ? product.getStock() : 0);
        sku.setStatus(1);
        sku.setImage(product.getMainImage());
        skuMapper.insert(sku);
    }

    @Override
    public void delete(Long id) {
        if (productMapper.selectById(id) == null) {
            throw new BusinessException(404, "商品不存在");
        }
        productMapper.deleteById(id);
        skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
    }

    @Override
    public void toggleSkuStatus(Long productId, Long skuId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值无效");
        }
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || !sku.getProductId().equals(productId)) {
            throw new BusinessException(404, "规格不存在");
        }
        sku.setStatus(status);
        skuMapper.updateById(sku);
    }

    @Override
    public void deleteSku(Long productId, Long skuId) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || !sku.getProductId().equals(productId)) {
            throw new BusinessException(404, "规格不存在");
        }
        skuMapper.deleteById(skuId);
    }
}
