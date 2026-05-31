package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping("/coupons")
    public Result<PageResult<Coupon>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getUserId();
        return Result.success(couponService.getAvailableCoupons(userId, page, pageSize));
    }

    @PostMapping("/coupons/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        couponService.receiveCoupon(userId, id);
        return Result.success();
    }

    @GetMapping("/user/coupons")
    public Result<PageResult<UserCoupon>> getUserCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        return Result.success(couponService.getUserCoupons(userId, status, page, pageSize));
    }

    @GetMapping("/user/coupons/available")
    public Result<List<UserCoupon>> getAvailableForOrder(
            @RequestParam(defaultValue = "0") BigDecimal amount) {
        Long userId = UserContext.getUserId();
        return Result.success(couponService.getAvailableForOrder(userId, amount));
    }
}
