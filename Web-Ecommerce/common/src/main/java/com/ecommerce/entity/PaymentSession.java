package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("payment_session")
public class PaymentSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String payMethod;
    private String qrToken;
    private Integer qrScanned;
    private LocalDateTime scanTime;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
