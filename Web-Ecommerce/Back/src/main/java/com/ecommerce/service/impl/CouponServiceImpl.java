package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.mapper.CouponMapper;
import com.ecommerce.mapper.UserCouponMapper;
import com.ecommerce.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;

    @Override
    public PageResult<Coupon> getAvailableCoupons(Long userId, int page, int pageSize) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .gt(Coupon::getRemainQty, 0)
                .last("ORDER BY type ASC, CASE type WHEN 2 THEN discount WHEN 1 THEN 100000-discount END ASC");

        Page<Coupon> result = couponMapper.selectPage(new Page<>(page, pageSize), wrapper);

        if (!result.getRecords().isEmpty() && userId != null) {
            List<Long> couponIds = result.getRecords().stream()
                    .map(Coupon::getId).toList();
            List<UserCoupon> received = userCouponMapper.selectList(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .in(UserCoupon::getCouponId, couponIds));
            Set<Long> receivedIds = received.stream()
                    .map(UserCoupon::getCouponId)
                    .collect(Collectors.toSet());
            for (Coupon c : result.getRecords()) {
                c.setReceived(receivedIds.contains(c.getId()));
            }
        }

        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("优惠券不存在或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("不在优惠券有效期内");
        }
        if (coupon.getRemainQty() <= 0) {
            throw new BusinessException("优惠券已被领完");
        }

        Long count = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId));
        if (count > 0) {
            throw new BusinessException("您已领取过该优惠券");
        }

        int affected = couponMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Coupon>()
                        .eq(Coupon::getId, couponId)
                        .gt(Coupon::getRemainQty, 0)
                        .setSql("remain_qty = remain_qty - 1"));
        if (affected == 0) {
            throw new BusinessException("优惠券已被领完");
        }

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        userCouponMapper.insert(uc);

        log.info("Coupon received: userId={}, couponId={}", userId, couponId);
    }
}
