package com.ecommerce.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeductStockRequest {
    private Long productId;
    private Long skuId;
    private Integer quantity;
}
