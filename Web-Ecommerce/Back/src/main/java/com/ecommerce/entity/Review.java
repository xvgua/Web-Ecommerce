package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private Long productId;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
    private Integer isFollowup;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
