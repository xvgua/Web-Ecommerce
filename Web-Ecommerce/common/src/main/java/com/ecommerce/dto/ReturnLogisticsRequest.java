package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnLogisticsRequest {
    @NotBlank(message = "物流公司不能为空")
    private String company;

    @NotBlank(message = "物流单号不能为空")
    private String logisticsNo;
}
