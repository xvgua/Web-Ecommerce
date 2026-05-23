package com.ecommerce.dto;

import lombok.Data;

@Data
public class OrderQuery {
    private Integer page = 1;
    private Integer pageSize = 20;
    private Integer status;
    private String reviewFilter;   // pending | followup | reviewed
}
