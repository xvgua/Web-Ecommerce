package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackReplyRequest {
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    @NotBlank(message = "回复内容不能为空")
    private String adminReply;
}
