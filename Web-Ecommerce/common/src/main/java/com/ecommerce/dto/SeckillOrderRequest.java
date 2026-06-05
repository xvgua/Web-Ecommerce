package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SeckillOrderRequest {
    @NotNull(message = "秒杀商品不能为空")
    private Long seckillProductId;
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
    private List<Long> userCouponIds;
    private String remark;
}
