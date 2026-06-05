package com.ecommerce.dto;

import lombok.Data;

@Data
public class UpdateCartRequest {
    private Integer quantity;
    private Boolean checked;
    private Long skuId;
}
