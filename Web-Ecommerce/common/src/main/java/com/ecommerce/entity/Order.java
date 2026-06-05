package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private BigDecimal totalAmount;
    private String couponIds;
    private BigDecimal couponDiscount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer status;
    private String remark;
    private Integer refundType;
    private String refundReason;
    private String refundDesc;
    private java.math.BigDecimal refundAmount;
    private String refundItemIds;
    private Integer refundStatus;
    private String refundRejectReason;
    private LocalDateTime refundApplyTime;
    private LocalDateTime refundDealTime;
    private LocalDateTime payTime;
    private LocalDateTime dealTime;
    private Integer addressModified;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private List<OrderItem> items;
    @TableField(exist = false)
    private Object address;
    @TableField(exist = false)
    private Long reviewCount;
    @TableField(exist = false)
    private String couponName;
    @TableField(exist = false)
    private String refundReasonText;
    @TableField(exist = false)
    private String refundStatusText;
    @TableField(exist = false)
    private java.util.List<OrderItem> refundItems;
}
