package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CouponForm;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    PageResult<Coupon> getAvailableCoupons(Long userId, int page, int pageSize);
    void receiveCoupon(Long userId, Long couponId);

    PageResult<UserCoupon> getUserCoupons(Long userId, Integer status, int page, int pageSize);
    List<UserCoupon> getAvailableForOrder(Long userId, BigDecimal orderAmount);
    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount);
    void markAsUsed(Long userCouponId, Long orderId);
    void releaseCoupon(Long userCouponId);

    // Admin methods
    PageResult<Coupon> adminGetPage(int page, int pageSize, String keyword, Integer type, Integer status);
    Coupon adminGetById(Long id);
    Coupon adminCreate(CouponForm form);
    void adminUpdate(Long id, CouponForm form);
    void adminDelete(Long id);
    void adminToggleStatus(Long id, Integer status);
}
