package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_product")
public class SeckillProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long productId;
    private Long skuId;              // 0=使用商品级别库存
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer remainStock;
    private Integer limitPerUser;    // 每人限购数量，默认1
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String productImage;
    @TableField(exist = false)
    private BigDecimal originalPrice;
    @TableField(exist = false)
    private String specDesc;
}
