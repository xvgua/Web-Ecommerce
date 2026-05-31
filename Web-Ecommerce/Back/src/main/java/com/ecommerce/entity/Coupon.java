package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal discount;
    private BigDecimal minAmount;
    private Integer totalQty;
    private Integer remainQty;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime grabStartTime;
    private LocalDateTime grabEndTime;
    private Integer scopeType;
    private String scopeIds;
    private Integer isLarge;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private Boolean received;
    @TableField(exist = false)
    private Integer userCouponStatus;
    @TableField(exist = false)
    private String scopeName;
    @TableField(exist = false)
    private String grabStatus;
    @TableField(exist = false)
    private Long grabCountdown;
}
