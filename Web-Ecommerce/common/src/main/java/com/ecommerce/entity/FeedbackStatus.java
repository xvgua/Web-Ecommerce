package com.ecommerce.entity;

public class FeedbackStatus {
    public static final int PENDING    = 0;
    public static final int PROCESSING = 1;
    public static final int RESOLVED   = 2;
    public static final int CLOSED     = 3;

    private FeedbackStatus() {}

    public static String getText(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING    -> "待处理";
            case PROCESSING -> "处理中";
            case RESOLVED   -> "已解决";
            case CLOSED     -> "已关闭";
            default -> "未知";
        };
    }
}
