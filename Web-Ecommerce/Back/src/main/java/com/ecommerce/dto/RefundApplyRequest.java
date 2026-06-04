package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class RefundApplyRequest {
    @NotNull(message = "退款类型不能为空")
    private Integer refundType;

    @NotNull(message = "退款原因不能为空")
    private String refundReason;

    @Size(max = 500, message = "补充说明不能超过500字")
    private String refundDesc;

    @NotNull(message = "退款商品不能为空")
    @Size(min = 1, message = "至少选择一件退款商品")
    private List<Long> itemIds;
}
