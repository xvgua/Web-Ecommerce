package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.Coupon;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping
    public Result<PageResult<Coupon>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getUserId();
        return Result.success(couponService.getAvailableCoupons(userId, page, pageSize));
    }

    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        couponService.receiveCoupon(userId, id);
        return Result.success();
    }
}
