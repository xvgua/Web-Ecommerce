package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateFollowUpReviewRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Long orderId;

    @NotBlank
    @Size(max = 1000)
    private String content;

    private List<String> images;
}
