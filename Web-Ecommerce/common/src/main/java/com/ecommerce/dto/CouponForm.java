package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponForm {
    @NotBlank(message = "券名称不能为空")
    private String name;

    @NotNull(message = "优惠类型不能为空")
    @Min(1) @Max(3)
    private Integer type;

    @NotNull(message = "优惠值不能为空")
    private BigDecimal discount;

    private BigDecimal minAmount;

    @NotNull(message = "发行总量不能为空")
    @Min(1)
    private Integer totalQty;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private LocalDateTime grabStartTime;
    private LocalDateTime grabEndTime;

    @NotNull(message = "适用范围不能为空")
    private Integer scopeType;

    private String scopeIds;

    private Integer isLarge;
    private Integer stackable;

    private Integer status;
}
