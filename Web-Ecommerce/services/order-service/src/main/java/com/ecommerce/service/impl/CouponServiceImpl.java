package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CouponForm;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.UserCoupon;
import com.ecommerce.mapper.CouponMapper;
import com.ecommerce.mapper.UserCouponMapper;
import com.ecommerce.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            java.util.Map<Long, UserCoupon> ucMap = received.stream()
                    .collect(Collectors.toMap(UserCoupon::getCouponId, uc -> uc, (a, b) -> a));
            for (Coupon c : result.getRecords()) {
                UserCoupon uc = ucMap.get(c.getId());
                if (uc != null) {
                    c.setReceived(true);
                    c.setUserCouponStatus(uc.getStatus());
                }
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

    @Override
    public PageResult<UserCoupon> getUserCoupons(Long userId, Integer status, int page, int pageSize) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreateTime);

        Page<UserCoupon> result = userCouponMapper.selectPage(new Page<>(page, pageSize), wrapper);
        if (!result.getRecords().isEmpty()) {
            List<Long> couponIds = result.getRecords().stream()
                    .map(UserCoupon::getCouponId).toList();
            List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
            java.util.Map<Long, Coupon> couponMap = coupons.stream()
                    .collect(Collectors.toMap(Coupon::getId, c -> c));
            for (UserCoupon uc : result.getRecords()) {
                uc.setCoupon(couponMap.get(uc.getCouponId()));
            }
        }
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public List<UserCoupon> getAvailableForOrder(Long userId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0));

        if (userCoupons.isEmpty()) return List.of();

        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).toList();
        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .in(Coupon::getId, couponIds)
                        .eq(Coupon::getStatus, 1)
                        .le(Coupon::getStartTime, now)
                        .ge(Coupon::getEndTime, now));

        java.util.Map<Long, Coupon> couponMap = coupons.stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        return userCoupons.stream()
                .filter(uc -> {
                    Coupon c = couponMap.get(uc.getCouponId());
                    if (c == null) return false;
                    return orderAmount.compareTo(c.getMinAmount()) >= 0;
                })
                .peek(uc -> uc.setCoupon(couponMap.get(uc.getCouponId())))
                .toList();
    }

    @Override
    public BigDecimal calculateTotalDiscount(List<Long> userCouponIds, BigDecimal orderAmount) {
        if (userCouponIds == null || userCouponIds.isEmpty()) return BigDecimal.ZERO;

        List<UserCoupon> ucs = userCouponMapper.selectBatchIds(userCouponIds);
        if (ucs.size() != userCouponIds.size()) {
            throw new BusinessException("部分优惠券不存在");
        }
        List<Long> couponIds = ucs.stream().map(UserCoupon::getCouponId).toList();
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        java.util.Map<Long, Coupon> couponMap = coupons.stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        LocalDateTime now = LocalDateTime.now();
        boolean hasNonStackable = false;

        for (UserCoupon uc : ucs) {
            if (uc.getStatus() != 0) throw new BusinessException("优惠券已使用或已过期");
            Coupon c = couponMap.get(uc.getCouponId());
            if (c == null || c.getStatus() != 1) throw new BusinessException("优惠券已失效");
            if (now.isBefore(c.getStartTime()) || now.isAfter(c.getEndTime()))
                throw new BusinessException("优惠券不在有效期内");
            if (orderAmount.compareTo(c.getMinAmount()) < 0)
                throw new BusinessException("订单金额未达到优惠券「" + c.getName() + "」使用门槛");
            if (c.getStackable() == null || c.getStackable() == 0) {
                hasNonStackable = true;
            }
        }

        // Non-stackable check: if any coupon is non-stackable, only 1 allowed
        if (hasNonStackable && userCouponIds.size() > 1) {
            throw new BusinessException("不可叠加的优惠券只能单独使用");
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (UserCoupon uc : ucs) {
            Coupon c = couponMap.get(uc.getCouponId());
            if (c.getType() == 1) {
                totalDiscount = totalDiscount.add(c.getDiscount());
            } else if (c.getType() == 2) {
                totalDiscount = totalDiscount.add(
                        orderAmount.multiply(BigDecimal.ONE.subtract(c.getDiscount()))
                );
            }
        }
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public void markAsUsed(List<Long> userCouponIds, Long orderId) {
        for (Long id : userCouponIds) {
            int affected = userCouponMapper.update(null,
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getId, id)
                            .eq(UserCoupon::getStatus, 0)
                            .set(UserCoupon::getStatus, 1)
                            .set(UserCoupon::getUseOrderId, orderId)
                            .set(UserCoupon::getUsedTime, LocalDateTime.now()));
            if (affected == 0) {
                throw new BusinessException("优惠券使用失败");
            }
        }
    }

    @Override
    @Transactional
    public void releaseCoupons(String couponIds) {
        if (couponIds == null || couponIds.isBlank()) return;
        for (String idStr : couponIds.split(",")) {
            Long id = Long.parseLong(idStr.trim());
            userCouponMapper.update(null,
                    new LambdaUpdateWrapper<UserCoupon>()
                            .eq(UserCoupon::getId, id)
                            .set(UserCoupon::getStatus, 0)
                            .set(UserCoupon::getUseOrderId, null)
                            .set(UserCoupon::getUsedTime, null));
        }
    }

    @Override
    public PageResult<Coupon> adminGetPage(int page, int pageSize, String keyword, Integer type, Integer status) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Coupon::getName, keyword.trim());
        }
        if (type != null) {
            wrapper.eq(Coupon::getType, type);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        Page<Coupon> result = couponMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public Coupon adminGetById(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        return coupon;
    }

    @Override
    @Transactional
    public Coupon adminCreate(CouponForm form) {
        Coupon coupon = new Coupon();
        coupon.setName(form.getName());
        coupon.setType(form.getType());
        coupon.setDiscount(form.getDiscount());
        coupon.setMinAmount(form.getMinAmount() != null ? form.getMinAmount() : BigDecimal.ZERO);
        coupon.setTotalQty(form.getTotalQty());
        coupon.setRemainQty(form.getTotalQty());
        coupon.setStartTime(form.getStartTime());
        coupon.setEndTime(form.getEndTime());
        coupon.setGrabStartTime(form.getGrabStartTime());
        coupon.setGrabEndTime(form.getGrabEndTime());
        coupon.setScopeType(form.getScopeType() != null ? form.getScopeType() : 1);
        coupon.setScopeIds(form.getScopeIds() != null ? form.getScopeIds() : "");
        coupon.setIsLarge(form.getIsLarge() != null ? form.getIsLarge() : 0);
        coupon.setStackable(form.getStackable() != null ? form.getStackable() : 0);
        coupon.setStatus(form.getStatus() != null ? form.getStatus() : 1);
        couponMapper.insert(coupon);
        log.info("Coupon admin created: id={}, name={}", coupon.getId(), coupon.getName());
        return coupon;
    }

    @Override
    @Transactional
    public void adminUpdate(Long id, CouponForm form) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        coupon.setName(form.getName());
        coupon.setType(form.getType());
        coupon.setDiscount(form.getDiscount());
        coupon.setMinAmount(form.getMinAmount() != null ? form.getMinAmount() : BigDecimal.ZERO);
        coupon.setStartTime(form.getStartTime());
        coupon.setEndTime(form.getEndTime());
        coupon.setGrabStartTime(form.getGrabStartTime());
        coupon.setGrabEndTime(form.getGrabEndTime());
        coupon.setScopeType(form.getScopeType() != null ? form.getScopeType() : 1);
        coupon.setScopeIds(form.getScopeIds() != null ? form.getScopeIds() : "");
        coupon.setIsLarge(form.getIsLarge() != null ? form.getIsLarge() : 0);
        coupon.setStackable(form.getStackable() != null ? form.getStackable() : 0);
        coupon.setStatus(form.getStatus() != null ? form.getStatus() : 1);
        // Update totalQty only if increased
        if (form.getTotalQty() > coupon.getTotalQty()) {
            int diff = form.getTotalQty() - coupon.getTotalQty();
            coupon.setTotalQty(form.getTotalQty());
            coupon.setRemainQty(coupon.getRemainQty() + diff);
        }
        couponMapper.updateById(coupon);
        log.info("Coupon admin updated: id={}", id);
    }

    @Override
    @Transactional
    public void adminDelete(Long id) {
        couponMapper.deleteById(id);
        log.info("Coupon admin deleted: id={}", id);
    }

    @Override
    @Transactional
    public void adminToggleStatus(Long id, Integer status) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
        log.info("Coupon status toggled: id={}, status={}", id, status);
    }
}
