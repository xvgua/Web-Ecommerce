package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductStatus;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import com.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Keyword Search")
class ProductServiceSearchTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper skuMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private SearchLogService searchLogService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product dell;
    private Product macbook;

    @BeforeEach
    void setUp() {
        dell = new Product();
        dell.setId(10L);
        dell.setName("Dell XPS 15");
        dell.setStatus(ProductStatus.ON_SALE);
        dell.setSales(45);

        macbook = new Product();
        macbook.setId(8L);
        macbook.setName("MacBook Pro 14 M3 Pro");
        macbook.setStatus(ProductStatus.ON_SALE);
        macbook.setSales(156);
    }

    @Test
    @DisplayName("有 keyword 时应走关键词搜索（FULLTEXT + LIKE）")
    void shouldUseKeywordSearch() {
        ProductQuery query = new ProductQuery();
        query.setKeyword("Dell");

        when(productMapper.countByKeyword(anyString(), anyString(), anyInt(), any()))
                .thenReturn(2L);
        when(productMapper.searchByKeyword(anyString(), anyString(), anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dell, macbook));
        when(skuMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResult<Product> result = productService.getProductPage(query);

        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        verify(productMapper).countByKeyword(anyString(), anyString(), anyInt(), any());
        verify(productMapper).searchByKeyword(anyString(), anyString(), anyInt(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("无 keyword 时应走筛选搜索，不触发关键词搜索")
    void shouldNotUseKeywordSearchWhenNoKeyword() {
        ProductQuery query = new ProductQuery();

        lenient().when(productMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20));
        lenient().when(skuMapper.selectList(any())).thenReturn(Collections.emptyList());

        productService.getProductPage(query);

        verify(productMapper, never()).countByKeyword(anyString(), anyString(), anyInt(), any());
        verify(productMapper, never()).searchByKeyword(anyString(), anyString(), anyInt(), any(), anyInt(), anyInt());
    }
}
