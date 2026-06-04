package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController 搜索场景")
class ProductControllerSearchTest {

    @Mock
    private ProductService productService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ProductController controller;

    @Test
    @DisplayName("keyword 应传递到 service")
    void shouldPassKeywordToService() {
        when(productService.getProductPage(any())).thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));

        ProductQuery query = new ProductQuery();
        query.setKeyword("手机");
        Result<PageResult<Product>> result = controller.list(query);

        assertEquals(200, result.getCode());

        ArgumentCaptor<ProductQuery> captor = ArgumentCaptor.forClass(ProductQuery.class);
        verify(productService).getProductPage(captor.capture());
        assertEquals("手机", captor.getValue().getKeyword());
    }

    @Test
    @DisplayName("categoryId 应传递到 service")
    void shouldPassCategoryId() {
        when(productService.getProductPage(any())).thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));

        ProductQuery query = new ProductQuery();
        query.setCategoryId(1L);
        controller.list(query);

        ArgumentCaptor<ProductQuery> captor = ArgumentCaptor.forClass(ProductQuery.class);
        verify(productService).getProductPage(captor.capture());
        assertEquals(1L, captor.getValue().getCategoryId());
    }

    @Test
    @DisplayName("sort 参数应传递到 service")
    void shouldPassSortParam() {
        when(productService.getProductPage(any())).thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));

        ProductQuery query = new ProductQuery();
        query.setSort("price_asc");
        controller.list(query);

        ArgumentCaptor<ProductQuery> captor = ArgumentCaptor.forClass(ProductQuery.class);
        verify(productService).getProductPage(captor.capture());
        assertEquals("price_asc", captor.getValue().getSort());
    }

    @Test
    @DisplayName("默认分页信息应正确返回")
    void shouldReturnPaginationResult() {
        PageResult<Product> mockPage = PageResult.of(Collections.emptyList(), 0, 1, 20);
        when(productService.getProductPage(any())).thenReturn(mockPage);

        ProductQuery query = new ProductQuery();
        Result<PageResult<Product>> result = controller.list(query);

        assertEquals(200, result.getCode());
        assertEquals(0, result.getData().getTotal());
        assertEquals(1, result.getData().getPage());
        assertEquals(20, result.getData().getPageSize());
    }
}
