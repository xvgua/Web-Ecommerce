package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_comment")
public class ReviewComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long userId;
    private String username;
    private String avatar;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
