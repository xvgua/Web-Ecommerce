package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer gender;
    private String intro;
    private LocalDateTime usernameUpdateTime;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
