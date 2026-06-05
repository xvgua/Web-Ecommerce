package com.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalesTrendDTO {
    private String date;
    private Long orderCount;
    private BigDecimal salesAmount;

    public SalesTrendDTO(String date, Long orderCount, BigDecimal salesAmount) {
        this.date = date;
        this.orderCount = orderCount;
        this.salesAmount = salesAmount;
    }
}
