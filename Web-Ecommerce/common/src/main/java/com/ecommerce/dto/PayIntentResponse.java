package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PayIntentResponse {
    private String qrToken;
    private String orderNo;
    private BigDecimal amount;
    private String payMethod;
}
