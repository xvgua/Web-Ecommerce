package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductForm;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;

import java.util.List;

public interface ProductService {
    PageResult<Product> getProductPage(ProductQuery query);
    Product getProductById(Long id);
    List<Product> getHotProducts(int limit);
    List<Product> getNewProducts(int limit);
    Product create(ProductForm form);
    void update(Long id, ProductForm form);
    void delete(Long id);
    void toggleSkuStatus(Long productId, Long skuId, Integer status);
    void deleteSku(Long productId, Long skuId);
}
