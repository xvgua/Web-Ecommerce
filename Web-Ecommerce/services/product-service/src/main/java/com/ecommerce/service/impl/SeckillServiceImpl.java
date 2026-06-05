package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.SeckillActivityForm;
import com.ecommerce.entity.*;
import com.ecommerce.mapper.*;
import com.ecommerce.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillActivityMapper activityMapper;
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;

    @Override
    public List<SeckillActivity> getActiveActivities() {
        syncActivityStatuses();
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 1)
                        .orderByAsc(SeckillActivity::getStartTime));
        for (SeckillActivity activity : activities) {
            fillActivityProducts(activity);
        }
        return activities;
    }

    @Override
    public SeckillActivity getActivityDetail(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) throw new BusinessException(404, "秒杀活动不存在");
        fillActivityProducts(activity);
        return activity;
    }

    @Override
    public SeckillProduct getSeckillProductDetail(Long seckillProductId) {
        SeckillProduct sp = seckillProductMapper.selectById(seckillProductId);
        if (sp == null) throw new BusinessException(404, "秒杀商品不存在");
        Product product = productMapper.selectById(sp.getProductId());
        if (product != null) {
            sp.setProductName(product.getName());
            sp.setProductImage(product.getMainImage());
            sp.setOriginalPrice(product.getPrice());
        }
        if (sp.getSkuId() != null && sp.getSkuId() > 0) {
            ProductSku sku = skuMapper.selectById(sp.getSkuId());
            if (sku != null) {
                sp.setSpecDesc(sku.getSpecName() + ":" + sku.getSpecValue());
                if (sku.getPrice() != null) sp.setOriginalPrice(sku.getPrice());
            }
        }
        return sp;
    }

    @Override
    public PageResult<SeckillActivity> adminGetPage(int page, int pageSize, String keyword, Integer status) {
        syncActivityStatuses();
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) wrapper.like(SeckillActivity::getName, keyword.trim());
        if (status != null) wrapper.eq(SeckillActivity::getStatus, status);
        wrapper.orderByDesc(SeckillActivity::getCreateTime);
        Page<SeckillActivity> result = activityMapper.selectPage(new Page<>(page, pageSize), wrapper);
        for (SeckillActivity activity : result.getRecords()) fillActivityProducts(activity);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public SeckillActivity adminGetById(Long id) {
        SeckillActivity activity = activityMapper.selectById(id);
        if (activity == null) throw new BusinessException(404, "秒杀活动不存在");
        fillActivityProducts(activity);
        return activity;
    }

    @Override
    @Transactional
    public SeckillActivity adminCreate(SeckillActivityForm form) {
        validateNoTimeOverlap(form.getStartTime(), form.getEndTime(), null);
        SeckillActivity activity = new SeckillActivity();
        activity.setName(form.getName());
        activity.setStartTime(form.getStartTime());
        activity.setEndTime(form.getEndTime());
        activity.setStatus(0);
        activityMapper.insert(activity);
        for (SeckillActivityForm.SeckillProductForm pf : form.getProducts()) {
            SeckillProduct sp = new SeckillProduct();
            sp.setActivityId(activity.getId()); sp.setProductId(pf.getProductId());
            sp.setSkuId(pf.getSkuId() != null ? pf.getSkuId() : 0L);
            sp.setSeckillPrice(pf.getSeckillPrice()); sp.setSeckillStock(pf.getSeckillStock());
            sp.setRemainStock(pf.getSeckillStock());
            sp.setLimitPerUser(pf.getLimitPerUser() != null ? pf.getLimitPerUser() : 1);
            seckillProductMapper.insert(sp);
        }
        log.info("Seckill activity created: id={}, name={}", activity.getId(), activity.getName());
        return activity;
    }

    @Override
    @Transactional
    public void adminUpdate(Long id, SeckillActivityForm form) {
        SeckillActivity activity = activityMapper.selectById(id);
        if (activity == null) throw new BusinessException(404, "秒杀活动不存在");
        validateNoTimeOverlap(form.getStartTime(), form.getEndTime(), id);
        activity.setName(form.getName()); activity.setStartTime(form.getStartTime());
        activity.setEndTime(form.getEndTime()); activityMapper.updateById(activity);
        seckillProductMapper.delete(new LambdaQueryWrapper<SeckillProduct>().eq(SeckillProduct::getActivityId, id));
        for (SeckillActivityForm.SeckillProductForm pf : form.getProducts()) {
            SeckillProduct sp = new SeckillProduct();
            sp.setActivityId(id); sp.setProductId(pf.getProductId());
            sp.setSkuId(pf.getSkuId() != null ? pf.getSkuId() : 0L);
            sp.setSeckillPrice(pf.getSeckillPrice()); sp.setSeckillStock(pf.getSeckillStock());
            sp.setRemainStock(pf.getSeckillStock());
            sp.setLimitPerUser(pf.getLimitPerUser() != null ? pf.getLimitPerUser() : 1);
            seckillProductMapper.insert(sp);
        }
        log.info("Seckill activity updated: id={}", id);
    }

    @Override
    @Transactional
    public void adminDelete(Long id) {
        seckillProductMapper.delete(new LambdaQueryWrapper<SeckillProduct>().eq(SeckillProduct::getActivityId, id));
        activityMapper.deleteById(id);
        log.info("Seckill activity deleted: id={}", id);
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void syncActivityStatuses() {
        LocalDateTime now = LocalDateTime.now();
        activityMapper.update(null, new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 0)
                .le(SeckillActivity::getStartTime, now)
                .gt(SeckillActivity::getEndTime, now)
                .set(SeckillActivity::getStatus, 1));
        activityMapper.update(null, new LambdaUpdateWrapper<SeckillActivity>()
                .ne(SeckillActivity::getStatus, 2)
                .lt(SeckillActivity::getEndTime, now)
                .set(SeckillActivity::getStatus, 2));
    }

    private void fillActivityProducts(SeckillActivity activity) {
        List<SeckillProduct> products = seckillProductMapper.selectList(
                new LambdaQueryWrapper<SeckillProduct>().eq(SeckillProduct::getActivityId, activity.getId()));
        for (SeckillProduct sp : products) {
            Product product = productMapper.selectById(sp.getProductId());
            if (product != null) {
                sp.setProductName(product.getName()); sp.setProductImage(product.getMainImage());
                sp.setOriginalPrice(product.getPrice());
            }
            if (sp.getSkuId() != null && sp.getSkuId() > 0) {
                ProductSku sku = skuMapper.selectById(sp.getSkuId());
                if (sku != null) {
                    sp.setSpecDesc(sku.getSpecName() + ":" + sku.getSpecValue());
                    if (sku.getPrice() != null) sp.setOriginalPrice(sku.getPrice());
                }
            }
        }
        activity.setProducts(products);
    }

    private void validateNoTimeOverlap(LocalDateTime start, LocalDateTime end, Long excludeId) {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        if (excludeId != null) wrapper.ne(SeckillActivity::getId, excludeId);
        wrapper.lt(SeckillActivity::getStartTime, end).gt(SeckillActivity::getEndTime, start);
        List<SeckillActivity> overlapping = activityMapper.selectList(wrapper);
        if (!overlapping.isEmpty())
            throw new BusinessException("当前时段已存在秒杀活动「" + overlapping.get(0).getName() + "」，同一时段仅允许一个活动");
    }
}
