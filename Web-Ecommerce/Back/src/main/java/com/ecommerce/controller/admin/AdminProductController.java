package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductForm;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

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

    @PostMapping
    public Result<Product> create(@Valid @RequestBody ProductForm form) {
        return Result.success(productService.create(form));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductForm form) {
        productService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
