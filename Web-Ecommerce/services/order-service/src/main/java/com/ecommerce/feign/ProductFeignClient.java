package com.ecommerce.feign;

import com.ecommerce.common.Result;
import com.ecommerce.dto.DeductStockRequest;
import com.ecommerce.dto.RestoreStockRequest;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductSku;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service", url = "${feign.product-service.url:http://localhost:8083}",
             fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    @GetMapping("/api/internal/products/{id}")
    Result<Product> getProductById(@PathVariable("id") Long id);

    @GetMapping("/api/internal/skus/{id}")
    Result<ProductSku> getSkuById(@PathVariable("id") Long id);

    @PostMapping("/api/internal/products/deduct-stock")
    Result<Void> deductStock(@RequestBody DeductStockRequest req);

    @PostMapping("/api/internal/products/restore-stock")
    Result<Void> restoreStock(@RequestBody RestoreStockRequest req);

    @GetMapping("/api/internal/products/batch")
    Result<List<Product>> batchGetProducts(@RequestParam("ids") List<Long> ids);

    @PutMapping("/api/internal/products/{id}/rating")
    Result<Void> updateRating(@PathVariable("id") Long id,
                              @RequestParam("avgRating") java.math.BigDecimal avgRating,
                              @RequestParam("reviewCount") Integer reviewCount);
}
