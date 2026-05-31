package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.entity.Coupon;

public interface CouponService {
    PageResult<Coupon> getAvailableCoupons(Long userId, int page, int pageSize);
    void receiveCoupon(Long userId, Long couponId);
}
