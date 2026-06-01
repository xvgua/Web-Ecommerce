package com.ecommerce.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
    private List<Long> cartItemIds;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private String remark;
    private List<Long> userCouponIds;
}
