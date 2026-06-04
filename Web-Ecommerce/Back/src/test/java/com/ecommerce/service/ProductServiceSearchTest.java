package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductStatus;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import com.ecommerce.service.impl.ProductServiceImpl;
import com.ecommerce.service.SearchLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Search Mode")
class ProductServiceSearchTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper skuMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private SearchLogService searchLogService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product phone;
    private Product phoneCase;

    @BeforeEach
    void setUp() {
        phone = new Product();
        phone.setId(4L);
        phone.setName("手机");
        phone.setStatus(ProductStatus.ON_SALE);

        phoneCase = new Product();
        phoneCase.setId(2L);
        phoneCase.setName("智能手机壳");
        phoneCase.setStatus(ProductStatus.ON_SALE);
    }

    @Nested
    @DisplayName("Fuzzy mode (default)")
    class FuzzyMode {

        @Test
        @DisplayName("should use three-tier keyword search when searchMode is null")
        void shouldDefaultToFuzzy() {
            ProductQuery query = new ProductQuery();
            query.setKeyword("手机");

            when(productMapper.countByKeyword(anyString(), anyString(), any(), anyInt(), any()))
                    .thenReturn(3L);
            when(productMapper.searchByKeyword(anyString(), anyString(), any(), anyInt(), any(), anyInt(), anyInt()))
                    .thenReturn(List.of(phone, phoneCase));
            when(skuMapper.selectList(any())).thenReturn(List.of());

            PageResult<Product> result = productService.getProductPage(query);

            assertEquals(3L, result.getTotal());
            assertEquals(2, result.getRecords().size());
            // Must use keyword search path
            verify(productMapper).countByKeyword(anyString(), anyString(), any(), anyInt(), any());
            verify(productMapper).searchByKeyword(anyString(), anyString(), any(), anyInt(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("should use three-tier keyword search when searchMode is 'fuzzy'")
        void shouldExplicitFuzzyMode() {
            ProductQuery query = new ProductQuery();
            query.setKeyword("手机");
            query.setSearchMode("fuzzy");

            when(productMapper.countByKeyword(anyString(), anyString(), any(), anyInt(), any()))
                    .thenReturn(2L);
            when(productMapper.searchByKeyword(anyString(), anyString(), any(), anyInt(), any(), anyInt(), anyInt()))
                    .thenReturn(List.of(phone, phoneCase));
            when(skuMapper.selectList(any())).thenReturn(List.of());

            PageResult<Product> result = productService.getProductPage(query);

            assertEquals(2, result.getRecords().size());
            verify(productMapper).searchByKeyword(anyString(), anyString(), any(), anyInt(), any(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("Exact mode")
    class ExactMode {

        @Test
        @DisplayName("should use exact match (not LIKE/FULLTEXT) when searchMode is 'exact'")
        void shouldUseExactMatch() {
            ProductQuery query = new ProductQuery();
            query.setKeyword("手机");
            query.setSearchMode("exact");

            Page<Product> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(phone));
            mockPage.setTotal(1L);

            when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(skuMapper.selectList(any())).thenReturn(List.of());

            PageResult<Product> result = productService.getProductPage(query);

            assertEquals(1, result.getRecords().size());
            assertEquals("手机", result.getRecords().get(0).getName());
            // Must NOT call the fuzzy keyword search methods
            verify(productMapper, never()).countByKeyword(anyString(), anyString(), any(), anyInt(), any());
            verify(productMapper, never()).searchByKeyword(anyString(), anyString(), any(), anyInt(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("should not match partial substring in exact mode")
        void shouldNotMatchPartial() {
            ProductQuery query = new ProductQuery();
            query.setKeyword("手机");
            query.setSearchMode("exact");

            Page<Product> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(phone)); // only "手机", not "智能手机壳"
            mockPage.setTotal(1L);

            when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            when(skuMapper.selectList(any())).thenReturn(List.of());

            PageResult<Product> result = productService.getProductPage(query);

            List<Product> records = result.getRecords();
            assertEquals(1, records.size());
            assertEquals("手机", records.get(0).getName());
            assertTrue(records.stream().noneMatch(p -> p.getName().equals("智能手机壳")));
        }
    }
}
