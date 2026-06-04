package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private String contact;
    private String images;
    private Integer status;
    private String adminReply;
    private Long adminId;
    private LocalDateTime handleTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String typeText;
    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String userEmail;
}
