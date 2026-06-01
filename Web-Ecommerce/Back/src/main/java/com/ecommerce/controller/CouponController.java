package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.CouponService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CouponController {

    @Autowired
    private CouponService couponService;
    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/coupons")
    public Result<PageResult<Coupon>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize,
                                            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        // GET /api/coupons is public (no LoginInterceptor), so try to parse token manually
        if (userId == null) {
            userId = tryParseUserId(request);
        }
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

    private Long tryParseUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            Claims claims = jwtUtils.parseToken(header.substring(7));
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }
}
