package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private String subject;
    private Integer sourceType;
    private Long sourceId;
    private String sourceName;
    private Integer status;
    private Integer unreadCount;
    private Integer userUnread;
    private String lastMessage;
    private LocalDateTime lastActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime closeTime;

    @TableField(exist = false)
    private Integer totalMessages;
}
