package com.ecommerce.entity;

public class OrderStatus {
    public static final int PENDING_PAY = 0;
    public static final int PENDING_SHIP = 1;
    public static final int SHIPPED = 2;
    public static final int COMPLETED = 3;
    public static final int CANCELLED = 4;
    public static final int REFUNDING = 5;
    public static final int REFUNDED = 6;

    private OrderStatus() {}
}
