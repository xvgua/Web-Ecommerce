package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.CouponForm;
import com.ecommerce.entity.Coupon;
import com.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping
    public Result<PageResult<Coupon>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer type,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(couponService.adminGetPage(page, pageSize, keyword, type, status));
    }

    @GetMapping("/{id}")
    public Result<Coupon> detail(@PathVariable Long id) {
        return Result.success(couponService.adminGetById(id));
    }

    @PostMapping
    public Result<Coupon> create(@Valid @RequestBody CouponForm form) {
        return Result.success(couponService.adminCreate(form));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponForm form) {
        couponService.adminUpdate(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.adminDelete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        couponService.adminToggleStatus(id, body.get("status"));
        return Result.success();
    }
}
