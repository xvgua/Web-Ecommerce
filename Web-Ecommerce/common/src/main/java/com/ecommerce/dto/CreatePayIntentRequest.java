package com.ecommerce.dto;

import lombok.Data;

@Data
public class CreatePayIntentRequest {
    private String payMethod;
}
