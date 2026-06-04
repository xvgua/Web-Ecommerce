package com.ecommerce.dto;

import lombok.Data;

@Data
public class RefundQuery extends PageQuery {
    private Integer refundStatus;
}
