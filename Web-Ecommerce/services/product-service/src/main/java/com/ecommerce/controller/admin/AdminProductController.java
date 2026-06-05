package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ImportResultDTO;
import com.ecommerce.dto.ProductForm;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PutMapping("/{productId}/skus/{skuId}/status")
    public Result<Void> toggleSkuStatus(@PathVariable Long productId,
                                         @PathVariable Long skuId,
                                         @RequestBody java.util.Map<String, Integer> body) {
        productService.toggleSkuStatus(productId, skuId, body.get("status"));
        return Result.success();
    }

    @DeleteMapping("/{productId}/skus/{skuId}")
    public Result<Void> deleteSku(@PathVariable Long productId, @PathVariable Long skuId) {
        productService.deleteSku(productId, skuId);
        return Result.success();
    }

    @GetMapping("/export")
    public void exportProducts(ProductQuery query, HttpServletResponse response) {
        productService.exportProducts(query, response);
    }

    @PostMapping("/import")
    public Result<ImportResultDTO> importProducts(@RequestParam("file") MultipartFile file) {
        return Result.success(productService.importProducts(file));
    }
}
