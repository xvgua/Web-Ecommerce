package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Result<PageResult<Product>> list(ProductQuery query) {
        return Result.success(productService.getProductPage(query));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @GetMapping("/hot")
    public Result<List<Product>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.getHotProducts(limit));
    }

    @GetMapping("/new")
    public Result<List<Product>> newProducts(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.getNewProducts(limit));
    }
}
