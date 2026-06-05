package com.ecommerce.entity;

public class FeedbackType {
    public static final int BUG_REPORT = 1;
    public static final int SUGGESTION = 2;

    private FeedbackType() {}

    public static String getText(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case BUG_REPORT -> "问题反馈";
            case SUGGESTION -> "功能建议";
            default -> "未知";
        };
    }
}
