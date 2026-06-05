package com.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HotProductDTO {
    private Long id;
    private String name;
    private String mainImage;
    private Long sales;
    private BigDecimal salesAmount;

    public HotProductDTO(Long id, String name, String mainImage, Long sales, BigDecimal salesAmount) {
        this.id = id;
        this.name = name;
        this.mainImage = mainImage;
        this.sales = sales;
        this.salesAmount = salesAmount;
    }
}
