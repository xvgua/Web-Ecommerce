package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundAuditRequest {
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String rejectReason;
}
