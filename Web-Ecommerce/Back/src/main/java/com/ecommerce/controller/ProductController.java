package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ReviewService reviewService;

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

    @GetMapping("/{id}/reviews")
    public Result<PageResult<Review>> reviews(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) Integer ratingMin,
                                              @RequestParam(required = false) Integer ratingMax) {
        return Result.success(reviewService.getProductReviews(id, page, pageSize, ratingMin, ratingMax));
    }
}
