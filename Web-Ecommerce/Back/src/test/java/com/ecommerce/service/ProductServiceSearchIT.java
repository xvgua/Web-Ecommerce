package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("ProductService 搜索集成测试 (H2)")
class ProductServiceSearchIT {

    @Autowired
    private ProductService productService;

    // ── Filter search (no keyword) ──

    @Test
    @DisplayName("分类筛选: 应按分类ID返回该分类下商品")
    void filterSearchByCategory() {
        ProductQuery query = new ProductQuery();
        query.setCategoryId(4L); // 电脑
        query.setStatus(1);

        PageResult<Product> result = productService.getProductPage(query);

        assertTrue(result.getTotal() >= 1);
        result.getRecords().forEach(p ->
                assertEquals(4L, p.getCategoryId()));
    }

    @Test
    @DisplayName("价格范围筛选: 应只返回价格区间内商品")
    void filterSearchByPriceRange() {
        ProductQuery query = new ProductQuery();
        query.setMinPrice(500.0);
        query.setMaxPrice(1000.0);

        PageResult<Product> result = productService.getProductPage(query);

        assertTrue(result.getTotal() >= 1);
        BigDecimal min = BigDecimal.valueOf(500.0);
        BigDecimal max = BigDecimal.valueOf(1000.0);
        result.getRecords().forEach(p -> {
            assertTrue(p.getPrice().compareTo(min) >= 0);
            assertTrue(p.getPrice().compareTo(max) <= 0);
        });
    }

    @Test
    @DisplayName("排序: price_desc 应按价格降序排列")
    void filterSearchSortByPriceDesc() {
        ProductQuery query = new ProductQuery();
        query.setSort("price_desc");

        PageResult<Product> result = productService.getProductPage(query);

        List<Product> records = result.getRecords();
        for (int i = 0; i < records.size() - 1; i++) {
            assertTrue(records.get(i).getPrice().compareTo(records.get(i + 1).getPrice()) >= 0,
                    "价格降序排列: " + records.get(i).getPrice() + " >= " + records.get(i + 1).getPrice());
        }
    }

    @Test
    @DisplayName("分页: 应正确分页返回")
    void filterSearchPagination() {
        ProductQuery query = new ProductQuery();
        query.setPage(1);
        query.setPageSize(2);

        PageResult<Product> result = productService.getProductPage(query);

        assertTrue(result.getRecords().size() <= 2);
        assertTrue(result.getTotal() >= result.getRecords().size());
    }

}
