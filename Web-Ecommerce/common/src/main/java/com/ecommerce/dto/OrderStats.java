package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStats {
    private long pendingPayment;
    private long pendingShipment;
    private long pendingReceipt;
    private long pendingReview;
}
