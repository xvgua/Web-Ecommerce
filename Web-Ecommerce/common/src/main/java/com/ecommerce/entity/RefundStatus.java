package com.ecommerce.entity;

public class RefundStatus {
    public static final int PENDING_REVIEW = 0;
    public static final int REJECTED       = 1;
    public static final int COMPLETED      = 2;
    public static final int CANCELLED      = 3;

    private RefundStatus() {}

    public static String getStatusText(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING_REVIEW -> "待审核";
            case REJECTED       -> "已拒绝";
            case COMPLETED      -> "已完成";
            case CANCELLED      -> "已撤销";
            default -> "未知";
        };
    }
}
