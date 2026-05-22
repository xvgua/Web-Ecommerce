package com.ecommerce.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
    @NotEmpty(message = "请选择要购买的商品")
    private List<Long> cartItemIds;
    private String remark;
}
