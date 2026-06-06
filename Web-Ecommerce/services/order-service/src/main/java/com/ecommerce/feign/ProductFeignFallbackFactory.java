package com.ecommerce.feign;

import com.ecommerce.common.Result;
import com.ecommerce.dto.CategorySalesDTO;
import com.ecommerce.dto.DeductStockRequest;
import com.ecommerce.dto.RestoreStockRequest;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductSku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("Product service unavailable", cause);
        return new ProductFeignClient() {
            @Override
            public Result<Product> getProductById(Long id) {
                return Result.error(503, "商品服务暂不可用，请稍后重试");
            }

            @Override
            public Result<ProductSku> getSkuById(Long id) {
                return Result.error(503, "商品服务暂不可用");
            }

            @Override
            public Result<Void> deductStock(DeductStockRequest req) {
                return Result.error(503, "库存服务暂不可用");
            }

            @Override
            public Result<Void> restoreStock(RestoreStockRequest req) {
                return Result.error(503, "库存服务暂不可用");
            }

            @Override
            public Result<List<Product>> batchGetProducts(List<Long> ids) {
                return Result.error(503, "商品服务暂不可用");
            }

            @Override
            public Result<Void> updateRating(Long id, java.math.BigDecimal avgRating, Integer reviewCount) {
                return Result.error(503, "商品服务暂不可用");
            }

            @Override
            public Result<List<CategorySalesDTO>> getCategorySales() {
                return Result.error(503, "商品服务暂不可用");
            }

            @Override
            public Result<List<Product>> getTopProductsBySales(int limit) {
                return Result.error(503, "商品服务暂不可用");
            }

            @Override
            public Result<Long> getProductCount() {
                return Result.error(503, "商品服务暂不可用");
            }
        };
    }
}
