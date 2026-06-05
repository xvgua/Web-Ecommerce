package com.ecommerce.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ImportResultDTO;
import com.ecommerce.dto.ProductExcelDTO;
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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            return keywordSearch(query);
        }
        return filterSearch(query);
    }

    private String beginSearch(ProductQuery query) {
        String keyword = query.getKeyword().trim();
        try {
            searchLogService.record(keyword, UserContext.getUserId());
        } catch (Exception e) {
            log.warn("Failed to record search log for keyword '{}': {}", keyword, e.getMessage());
        }
        return keyword;
    }

    private PageResult<Product> finishSearch(List<Product> records, long total, int page, int pageSize) {
        fillCategoryNames(records);
        fillSkus(records);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * Keyword search: FULLTEXT (ngram) + LIKE, with Java-side fuzzy matching
     * combining Levenshtein edit distance (typo tolerance) and longest prefix
     * matching (partial-word completion, e.g. "iph" → "iPhone").
     * Ordered by relevance: prefix match, LIKE match, then sales.
     */
    private PageResult<Product> keywordSearch(ProductQuery query) {
        String keyword = beginSearch(query);
        String escapedKeyword = escapeBooleanMode(keyword);
        String likeKeyword = escapeLikeWildcards(keyword);
        Integer status = query.getStatus() != null ? query.getStatus() : ProductStatus.ON_SALE;
        List<Long> categoryIds = query.getCategoryId() != null && query.getCategoryId() > 0
                ? new ArrayList<>(collectDescendantCategoryIds(query.getCategoryId())) : null;

        boolean doFuzzy = keyword.length() > 2;

        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        int offset = (page - 1) * pageSize;

        long total = productMapper.countByKeyword(escapedKeyword, likeKeyword, status, categoryIds);
        List<Product> records = productMapper.searchByKeyword(
                escapedKeyword, likeKeyword, status, categoryIds, offset, pageSize);

        if (doFuzzy) {
            // Levenshtein-based typo-tolerant matching
            List<Product> fuzzyMatches = fuzzyMatch(keyword, records, status, categoryIds);
            // Longest-prefix matching: catches partial-word prefixes
            // (e.g. "iph" → "iPhone 15", "Dell" → "Dell XPS")
            List<Product> prefixMatches = prefixMatch(keyword, records, status, categoryIds, fuzzyMatches);
            if (!fuzzyMatches.isEmpty() || !prefixMatches.isEmpty()) {
                records = mergeAndSort(records, fuzzyMatches, prefixMatches, likeKeyword, keyword);
                total += fuzzyMatches.size() + prefixMatches.size();
            }
        }

        return finishSearch(records, total, page, pageSize);
    }

    /**
     * Levenshtein edit distance — standard dynamic programming.
     */
    private int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }

    /**
     * Longest common prefix length between two strings.
     */
    private int commonPrefixLength(String a, String b) {
        int maxLen = Math.min(a.length(), b.length());
        int i = 0;
        while (i < maxLen && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    /**
     * Longest prefix match between keyword and any word in text.
     * Splits text on spaces and common delimiters, then finds the
     * word with the longest common prefix with the keyword.
     * e.g. keyword="iph", text="iPhone 15 Pro" → returns 3
     */
    private int longestPrefixMatch(String keyword, String text) {
        String lowerKw = keyword.toLowerCase();
        String lowerText = text.toLowerCase();
        int best = 0;
        int kwLen = lowerKw.length();
        for (String word : lowerText.split("[\\s\\-/|(),.]+")) {
            int match = commonPrefixLength(lowerKw, word);
            if (match > best) best = match;
            if (best == kwLen) break;
        }
        return best;
    }

    /**
     * Minimum edit distance between keyword and any substring of text.
     * Uses a sliding window: for each start position in text, tries
     * substrings of length keywordLen ± maxDist and keeps the minimum.
     */
    private int minSubstringDistance(String keyword, String text, int maxDist) {
        int kwLen = keyword.length();
        int txtLen = text.length();
        int best = kwLen; // worst case: delete all keyword chars

        for (int start = 0; start < txtLen; start++) {
            int minLen = Math.max(1, kwLen - maxDist);
            int maxLen = Math.min(txtLen - start, kwLen + maxDist);
            for (int len = minLen; len <= maxLen; len++) {
                int d = levenshtein(keyword, text.substring(start, start + len));
                if (d < best) best = d;
                if (best == 0) return 0; // exact match found, can't get better
            }
        }
        return best;
    }

    /**
     * Find active products whose name fuzzily contains the keyword.
     * Uses sliding-window Levenshtein: for each product, slides a window
     * across its name and finds the substring with minimum edit distance
     * to the keyword. This correctly handles cases like:
     *   "iphane" → "iPhone 15 Pro Max 256GB"  (matches "iPhone")
     *   "Dell"   → "Dell XPS 15"              (matches "Dell")
     */
    private List<Product> fuzzyMatch(String keyword, List<Product> exactResults,
                                      Integer status, List<Long> categoryIds) {
        Set<Long> existingIds = new HashSet<>();
        for (Product p : exactResults) existingIds.add(p.getId());

        List<Product> allActive = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, status)
                        .in(categoryIds != null && !categoryIds.isEmpty(), Product::getCategoryId, categoryIds));

        String lowerKw = keyword.toLowerCase();
        int kwLen = keyword.length();
        int maxDist = kwLen <= 4 ? 1 : 2; // ≤4 字符收紧，避免短子串碰巧命中
        List<Product> fuzzy = new ArrayList<>();
        for (Product p : allActive) {
            if (existingIds.contains(p.getId())) continue;
            if (p.getName() == null) continue;
            String name = p.getName().toLowerCase();
            if (minSubstringDistance(lowerKw, name, maxDist) <= maxDist) {
                fuzzy.add(p);
            }
        }
        return fuzzy;
    }

    /**
     * Find active products whose name contains a word starting with the
     * keyword (longest prefix matching). Requires at least 1-char prefix
     * match for short keywords (≤3 chars) and ≥2 chars for longer keywords.
     * <p>
     * This catches cases that FULLTEXT/ngram may miss, like:
     *   "iph" → "iPhone 15 Pro Max"  (prefix of "iphone")
     *   "dell" → "Dell XPS 15"       (prefix of "dell")
     */
    private List<Product> prefixMatch(String keyword, List<Product> exactResults,
                                       Integer status, List<Long> categoryIds,
                                       List<Product> fuzzyMatches) {
        Set<Long> existingIds = new HashSet<>();
        for (Product p : exactResults) existingIds.add(p.getId());
        for (Product p : fuzzyMatches) existingIds.add(p.getId());

        List<Product> allActive = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, status)
                        .in(categoryIds != null && !categoryIds.isEmpty(), Product::getCategoryId, categoryIds));

        String lowerKw = keyword.toLowerCase();
        int minPrefix = keyword.length() <= 3 ? 1 : 2;
        List<Product> result = new ArrayList<>();
        for (Product p : allActive) {
            if (existingIds.contains(p.getId())) continue;
            if (p.getName() == null) continue;
            if (longestPrefixMatch(lowerKw, p.getName()) >= minPrefix) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Merge exact matches + fuzzy matches + prefix matches, preserving
     * relevance order. Prefix-substring matches rank first (best signal
     * for partial-word queries), then LIKE-substring matches, then
     * fuzzy (Levenshtein) matches; within each group sorted by sales.
     */
    private List<Product> mergeAndSort(List<Product> exact, List<Product> fuzzy,
                                        List<Product> prefix, String likeKeyword,
                                        String keyword) {
        List<Product> result = new ArrayList<>(exact);
        result.addAll(fuzzy);
        result.addAll(prefix);
        String likeKw = likeKeyword.toLowerCase();
        String prefixKw = keyword.toLowerCase();
        result.sort((a, b) -> {
            // 1) Prefix match: keyword is a prefix of some word in product name
            boolean aPrefix = a.getName() != null && containsWordWithPrefix(a.getName().toLowerCase(), prefixKw);
            boolean bPrefix = b.getName() != null && containsWordWithPrefix(b.getName().toLowerCase(), prefixKw);
            if (aPrefix && !bPrefix) return -1;
            if (!aPrefix && bPrefix) return 1;
            // 2) LIKE substring match
            boolean aLike = a.getName() != null && a.getName().toLowerCase().contains(likeKw);
            boolean bLike = b.getName() != null && b.getName().toLowerCase().contains(likeKw);
            if (aLike && !bLike) return -1;
            if (!aLike && bLike) return 1;
            // 3) Longer prefix match wins within same group
            int aPrefixLen = a.getName() != null ? longestPrefixMatch(prefixKw, a.getName()) : 0;
            int bPrefixLen = b.getName() != null ? longestPrefixMatch(prefixKw, b.getName()) : 0;
            if (aPrefixLen != bPrefixLen) return Integer.compare(bPrefixLen, aPrefixLen);
            // 4) Sales descending
            return Integer.compare(b.getSales() != null ? b.getSales() : 0,
                                   a.getSales() != null ? a.getSales() : 0);
        });
        return result;
    }

    private boolean containsWordWithPrefix(String text, String prefix) {
        for (String word : text.split("[\\s\\-/|(),.]+")) {
            if (word.startsWith(prefix)) return true;
        }
        return false;
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

        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        Page<Product> result = productMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return finishSearch(result.getRecords(), result.getTotal(), page, pageSize);
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
                sku.setSpecValue(sf.getSpecValue() != null ? sf.getSpecValue() : "");
                sku.setPrice(sf.getPrice());
                sku.setStock(sf.getStock() != null ? sf.getStock() : 0);
                sku.setStatus(sf.getStatus() != null ? sf.getStatus() : 1);
                sku.setImage(sf.getImage() != null ? sf.getImage() : "");
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
                    sku.setSpecValue(sf.getSpecValue() != null ? sf.getSpecValue() : "");
                    sku.setPrice(sf.getPrice());
                    sku.setStock(sf.getStock() != null ? sf.getStock() : 0);
                    sku.setStatus(sf.getStatus() != null ? sf.getStatus() : 1);
                    sku.setImage(sf.getImage() != null ? sf.getImage() : "");
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

    @Override
    public void exportProducts(ProductQuery query, HttpServletResponse response) {
        List<Product> products = queryProductsForExport(query);
        List<ProductExcelDTO> rows = products.stream().map(this::toExcelDTO).collect(Collectors.toList());

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("商品列表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            EasyExcel.write(response.getOutputStream(), ProductExcelDTO.class).sheet("商品列表").doWrite(rows);
        } catch (IOException e) {
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    private List<Product> queryProductsForExport(ProductQuery query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Product::getName, query.getKeyword());
        }
        if (query.getCategoryId() != null && query.getCategoryId() > 0) {
            Set<Long> categoryIds = collectDescendantCategoryIds(query.getCategoryId());
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        wrapper.orderByDesc(Product::getId);
        wrapper.last("LIMIT 5000");
        return productMapper.selectList(wrapper);
    }

    private ProductExcelDTO toExcelDTO(Product p) {
        ProductExcelDTO dto = new ProductExcelDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCategoryName(p.getCategoryName());
        dto.setPrice(p.getPrice());
        dto.setStock(p.getStock());
        dto.setDescription(p.getDescription());
        dto.setStatusText(p.getStatus() != null && p.getStatus() == 1 ? "上架" : "下架");
        dto.setSales(p.getSales());

        // SKU summary
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, p.getId()));
        if (skus != null && !skus.isEmpty()) {
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (ProductSku sku : skus) {
                grouped.computeIfAbsent(sku.getSpecName(), k -> new ArrayList<>())
                        .add(sku.getSpecValue() != null && !sku.getSpecValue().isEmpty() ? sku.getSpecValue() : sku.getSpecName());
            }
            dto.setSkuSummary(grouped.entrySet().stream()
                    .map(e -> e.getKey() + ":" + String.join("/", e.getValue()))
                    .collect(Collectors.joining("; ")));
        }

        if (p.getCreateTime() != null) {
            dto.setCreateTime(p.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return dto;
    }

    @Override
    @Transactional
    public ImportResultDTO importProducts(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BusinessException("仅支持 .xlsx / .xls 格式的文件");
        }

        List<ProductExcelDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(ProductExcelDTO.class).sheet().doReadSync();
        } catch (Exception e) {
            throw new BusinessException("读取Excel文件失败: " + e.getMessage());
        }

        ImportResultDTO result = new ImportResultDTO();
        result.setTotalCount(Math.min(rows.size(), 500));

        // Preload category name -> id mapping
        List<Category> categories = categoryMapper.selectList(null);
        Map<String, Long> categoryNameMap = new HashMap<>();
        for (Category c : categories) {
            categoryNameMap.put(c.getName(), c.getId());
        }

        int limit = Math.min(rows.size(), 500);
        for (int i = 0; i < limit; i++) {
            ProductExcelDTO row = rows.get(i);
            int rowNum = i + 2; // Excel row number (1-based + header row)
            try {
                validateRow(row, categoryNameMap, rowNum);
                if (row.getId() != null) {
                    updateProductFromExcel(row, categoryNameMap);
                } else {
                    createProductFromExcel(row, categoryNameMap);
                }
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (BusinessException e) {
                result.setFailCount(result.getFailCount() + 1);
                result.addError(rowNum, e.getMessage());
            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                result.addError(rowNum, "系统错误: " + e.getMessage());
            }
        }

        return result;
    }

    private void validateRow(ProductExcelDTO row, Map<String, Long> categoryNameMap, int rowNum) {
        if (row.getName() == null || row.getName().isBlank()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (row.getPrice() == null || row.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于0");
        }
        if (row.getStock() == null || row.getStock() < 0) {
            throw new BusinessException("库存不能为空且不能为负数");
        }
        if (row.getCategoryName() != null && !row.getCategoryName().isBlank()
                && !categoryNameMap.containsKey(row.getCategoryName())) {
            throw new BusinessException("分类「" + row.getCategoryName() + "」不存在");
        }
        if (row.getStatusText() != null && !row.getStatusText().isBlank()
                && !"上架".equals(row.getStatusText()) && !"下架".equals(row.getStatusText())) {
            throw new BusinessException("状态只能填「上架」或「下架」");
        }
    }

    private void createProductFromExcel(ProductExcelDTO row, Map<String, Long> categoryNameMap) {
        Product product = new Product();
        product.setName(row.getName().trim());
        if (row.getCategoryName() != null && !row.getCategoryName().isBlank()) {
            product.setCategoryId(categoryNameMap.get(row.getCategoryName()));
        }
        product.setPrice(row.getPrice());
        product.setStock(row.getStock());
        product.setDescription(row.getDescription() != null ? row.getDescription() : "");
        int status = "上架".equals(row.getStatusText()) ? 1 : 0;
        product.setStatus(status);
        product.setSales(0);
        if (status == 1) {
            product.setListedAt(LocalDateTime.now());
        }
        productMapper.insert(product);
        createDefaultSku(product);
        log.info("Product created from import: id={}, name={}", product.getId(), product.getName());
    }

    private void updateProductFromExcel(ProductExcelDTO row, Map<String, Long> categoryNameMap) {
        Product product = productMapper.selectById(row.getId());
        if (product == null) {
            throw new BusinessException("商品ID " + row.getId() + " 不存在");
        }
        product.setName(row.getName().trim());
        if (row.getCategoryName() != null && !row.getCategoryName().isBlank()) {
            product.setCategoryId(categoryNameMap.get(row.getCategoryName()));
        }
        product.setPrice(row.getPrice());
        product.setStock(row.getStock());
        product.setDescription(row.getDescription() != null ? row.getDescription() : "");
        if (row.getStatusText() != null && !row.getStatusText().isBlank()) {
            int newStatus = "上架".equals(row.getStatusText()) ? 1 : 0;
            int oldStatus = product.getStatus() != null ? product.getStatus() : 0;
            product.setStatus(newStatus);
            if (newStatus == 1 && oldStatus != 1 && product.getListedAt() == null) {
                product.setListedAt(LocalDateTime.now());
            }
        }
        productMapper.updateById(product);
        log.info("Product updated from import: id={}, name={}", product.getId(), product.getName());
    }
}
