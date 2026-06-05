package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seckill_activity")
public class SeckillActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String backgroundImage;  // 活动背景图URL
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;          // 0=未开始 1=进行中 2=已结束
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private java.util.List<SeckillProduct> products;
}
