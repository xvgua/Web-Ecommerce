package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateReviewRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Long orderId;

    @NotNull(message = "描述相符评分不能为空")
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal ratingDesc;

    @NotNull(message = "物流服务评分不能为空")
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal ratingLogistics;

    @NotNull(message = "服务态度评分不能为空")
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal ratingService;

    @NotBlank
    @Size(max = 1000)
    private String content;

    private List<String> images;
}
