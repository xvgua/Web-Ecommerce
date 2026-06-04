package com.ecommerce.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class RefundReason {
    public static final String DONT_WANT        = "dont_want";
    public static final String WRONG_ITEM       = "wrong_item";
    public static final String NOT_AS_DESCRIBED = "not_as_described";
    public static final String DAMAGED          = "damaged";
    public static final String LATE_DELIVERY    = "late_delivery";
    public static final String OTHER            = "other";

    private static final Map<String, String> TEXT_MAP = new LinkedHashMap<>();
    static {
        TEXT_MAP.put(DONT_WANT, "不想要了");
        TEXT_MAP.put(WRONG_ITEM, "买错了");
        TEXT_MAP.put(NOT_AS_DESCRIBED, "商品与描述不符");
        TEXT_MAP.put(DAMAGED, "商品破损");
        TEXT_MAP.put(LATE_DELIVERY, "未按约定时间发货");
        TEXT_MAP.put(OTHER, "其他");
    }

    private RefundReason() {}

    public static String getReasonText(String reason) {
        if (reason == null) return "";
        return TEXT_MAP.getOrDefault(reason, reason);
    }

    public static Map<String, String> getAllReasons() {
        return TEXT_MAP;
    }
}
