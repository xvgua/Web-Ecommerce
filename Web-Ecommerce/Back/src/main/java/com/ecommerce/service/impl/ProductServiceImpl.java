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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    public PageResult<Product> getProductPage(ProductQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
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
        String escapedKeyword = escapeBooleanMode(keyword);
        String likeKeyword = escapeLikeWildcards(keyword);
        Integer status = query.getStatus() != null ? query.getStatus() : ProductStatus.ON_SALE;

        // Skip Levenshtein for very short keywords (≤2 chars):
        // edit distance ≤2 would match almost everything, making it noise
        String fuzzyKeyword = keyword.length() > 2 ? keyword.toLowerCase() : null;

        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        int offset = (page - 1) * pageSize;

        long total = productMapper.countByKeyword(escapedKeyword, likeKeyword, fuzzyKeyword, status);
        List<Product> records = productMapper.searchByKeyword(
                escapedKeyword, likeKeyword, fuzzyKeyword, status, offset, pageSize);

        fillCategoryNames(records);
        return PageResult.of(records, total, page, pageSize);
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
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
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
            wrapper.orderByDesc(Product::getCreateTime);
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
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ON_SALE)
                .orderByDesc(Product::getCreateTime)
                .last("LIMIT " + Math.min(limit, 50));
        return productMapper.selectList(wrapper);
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
        product.setStatus(form.getStatus() != null ? form.getStatus() : ProductStatus.ON_SALE);
        product.setSales(0);
        productMapper.insert(product);

        if (form.getSkus() != null && !form.getSkus().isEmpty()) {
            for (SkuForm sf : form.getSkus()) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setSpecName(sf.getSpecName());
                sku.setSpecValue(sf.getSpecValue());
                sku.setPrice(sf.getPrice());
                sku.setStock(sf.getStock() != null ? sf.getStock() : 0);
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
        if (form.getStatus() != null) product.setStatus(form.getStatus());
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
}
