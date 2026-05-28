package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SkuForm {
    @NotBlank(message = "规格名不能为空")
    private String specName;
    @NotBlank(message = "规格值不能为空")
    private String specValue;
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    private Integer stock;
    private String image;
}
