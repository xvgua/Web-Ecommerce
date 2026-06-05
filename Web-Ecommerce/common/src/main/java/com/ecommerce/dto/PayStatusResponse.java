package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayStatusResponse {
    private String status;
    private boolean scanned;
    private String payMethod;
}
