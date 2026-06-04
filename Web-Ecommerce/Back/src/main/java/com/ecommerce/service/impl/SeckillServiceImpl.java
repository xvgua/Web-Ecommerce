package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.SeckillActivityForm;
import com.ecommerce.dto.SeckillOrderRequest;
import com.ecommerce.entity.*;
import com.ecommerce.mapper.*;
import com.ecommerce.service.CouponService;
import com.ecommerce.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final String RATE_LIMIT_KEY = "seckill:ratelimit:";
    private static final long RATE_LIMIT_SECONDS = 3;

    @Autowired
    private SeckillActivityMapper activityMapper;
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private CouponService couponService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        if (activity == null) {
            throw new BusinessException(404, "秒杀活动不存在");
        }
        fillActivityProducts(activity);
        return activity;
    }

    @Override
    @Transactional
    public Order createSeckillOrder(Long userId, SeckillOrderRequest req) {
        // Rate limiting
        String rateKey = RATE_LIMIT_KEY + userId;
        String lastRequest = stringRedisTemplate.opsForValue().get(rateKey);
        if (lastRequest != null) {
            throw new BusinessException("操作太频繁，请稍后再试");
        }
        stringRedisTemplate.opsForValue().set(rateKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);

        SeckillProduct sp = seckillProductMapper.selectById(req.getSeckillProductId());
        if (sp == null) {
            throw new BusinessException("秒杀商品不存在");
        }

        SeckillActivity activity = activityMapper.selectById(sp.getActivityId());
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException("秒杀活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException("秒杀活动已结束");
        }

        if (sp.getRemainStock() <= 0) {
            throw new BusinessException("秒杀商品已售罄");
        }

        // Check per-user limit
        int limit = sp.getLimitPerUser() != null && sp.getLimitPerUser() > 0 ? sp.getLimitPerUser() : 1;
        Long boughtCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getProductId, sp.getProductId())
                        .inSql(OrderItem::getOrderId,
                                "SELECT id FROM `order` WHERE user_id = " + userId + " AND status NOT IN (4)"));
        // Simplified: check seckill orders specifically
        List<Order> userOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .ne(Order::getStatus, OrderStatus.CANCELLED));
        Set<Long> userOrderIds = userOrders.stream().map(Order::getId).collect(Collectors.toSet());
        if (!userOrderIds.isEmpty()) {
            Long seckillItemCount = orderItemMapper.selectCount(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getProductId, sp.getProductId())
                            .in(OrderItem::getOrderId, userOrderIds));
            if (seckillItemCount >= limit) {
                throw new BusinessException("该商品每人限购" + limit + "件");
            }
        }

        // Deduct seckill stock with optimistic lock
        int affected = seckillProductMapper.update(null,
                new LambdaUpdateWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getId, sp.getId())
                        .gt(SeckillProduct::getRemainStock, 0)
                        .setSql("remain_stock = remain_stock - 1"));
        if (affected == 0) {
            throw new BusinessException("秒杀商品已售罄");
        }

        // Also deduct product/SKU stock
        Product product = productMapper.selectById(sp.getProductId());
        if (product == null || product.getStatus() == ProductStatus.OFF_SHELF) {
            throw new BusinessException("商品已下架");
        }
        if (sp.getSkuId() != null && sp.getSkuId() > 0) {
            ProductSku sku = skuMapper.selectById(sp.getSkuId());
            if (sku == null || sku.getStock() < 1) {
                throw new BusinessException("商品库存不足");
            }
            skuMapper.update(null,
                    new LambdaUpdateWrapper<ProductSku>()
                            .eq(ProductSku::getId, sp.getSkuId())
                            .ge(ProductSku::getStock, 1)
                            .setSql("stock = stock - 1")
                            .setSql("sales = sales + 1"));
            productMapper.update(null,
                    new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, product.getId())
                            .setSql("sales = sales + 1"));
        } else {
            if (product.getStock() < 1) {
                throw new BusinessException("商品库存不足");
            }
            productMapper.update(null,
                    new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, product.getId())
                            .ge(Product::getStock, 1)
                            .setSql("stock = stock - 1")
                            .setSql("sales = sales + 1"));
        }

        // Validate address
        Address address = addressMapper.selectById(req.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        // Create order
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAddressId(req.getAddressId());
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setRemark(req.getRemark());

        String specDesc = "";
        String productImage = product.getMainImage();
        String productName = product.getName();
        if (sp.getSkuId() != null && sp.getSkuId() > 0) {
            ProductSku sku = skuMapper.selectById(sp.getSkuId());
            if (sku != null) {
                specDesc = sku.getSpecName() + ":" + sku.getSpecValue();
                productImage = sku.getImage() != null ? sku.getImage() : productImage;
            }
        }

        BigDecimal totalAmount = sp.getSeckillPrice();
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(productName);
        item.setProductImage(productImage);
        item.setSkuId(sp.getSkuId());
        item.setSpecDesc(specDesc);
        item.setQuantity(1);
        item.setPrice(sp.getSeckillPrice());
        orderItemMapper.insert(item);

        log.info("Seckill order created: orderNo={}, userId={}, seckillProductId={}, price={}",
                order.getOrderNo(), userId, sp.getId(), sp.getSeckillPrice());
        return order;
    }

    @Override
    public SeckillProduct getSeckillProductDetail(Long seckillProductId) {
        SeckillProduct sp = seckillProductMapper.selectById(seckillProductId);
        if (sp == null) {
            throw new BusinessException(404, "秒杀商品不存在");
        }
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
                if (sku.getPrice() != null) {
                    sp.setOriginalPrice(sku.getPrice());
                }
            }
        }
        return sp;
    }

    @Override
    public List<Long> getUserPurchasedProductIds(Long userId) {
        List<Long> result = new ArrayList<>();

        // Get non-cancelled orders for this user
        List<Order> userOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .ne(Order::getStatus, OrderStatus.CANCELLED));
        Set<Long> userOrderIds = userOrders.stream().map(Order::getId).collect(Collectors.toSet());

        if (userOrderIds.isEmpty()) return result;

        // Get active seckill products
        syncActivityStatuses();
        List<SeckillActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<SeckillActivity>().eq(SeckillActivity::getStatus, 1));
        for (SeckillActivity activity : activities) {
            List<SeckillProduct> products = seckillProductMapper.selectList(
                    new LambdaQueryWrapper<SeckillProduct>()
                            .eq(SeckillProduct::getActivityId, activity.getId()));
            for (SeckillProduct sp : products) {
                int limit = sp.getLimitPerUser() != null && sp.getLimitPerUser() > 0 ? sp.getLimitPerUser() : 1;
                Long boughtCount = orderItemMapper.selectCount(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getProductId, sp.getProductId())
                                .in(OrderItem::getOrderId, userOrderIds));
                if (boughtCount >= limit) {
                    result.add(sp.getId());
                }
            }
        }
        return result;
    }

    @Override
    public PageResult<SeckillActivity> adminGetPage(int page, int pageSize, String keyword, Integer status) {
        syncActivityStatuses();
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SeckillActivity::getName, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(SeckillActivity::getStatus, status);
        }
        wrapper.orderByDesc(SeckillActivity::getCreateTime);

        Page<SeckillActivity> result = activityMapper.selectPage(new Page<>(page, pageSize), wrapper);
        for (SeckillActivity activity : result.getRecords()) {
            fillActivityProducts(activity);
        }
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public SeckillActivity adminGetById(Long id) {
        SeckillActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(404, "秒杀活动不存在");
        }
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
            sp.setActivityId(activity.getId());
            sp.setProductId(pf.getProductId());
            sp.setSkuId(pf.getSkuId() != null ? pf.getSkuId() : 0L);
            sp.setSeckillPrice(pf.getSeckillPrice());
            sp.setSeckillStock(pf.getSeckillStock());
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
        if (activity == null) {
            throw new BusinessException(404, "秒杀活动不存在");
        }

        validateNoTimeOverlap(form.getStartTime(), form.getEndTime(), id);
        activity.setName(form.getName());
        activity.setStartTime(form.getStartTime());
        activity.setEndTime(form.getEndTime());
        activityMapper.updateById(activity);

        // Remove old products and insert new ones
        seckillProductMapper.delete(
                new LambdaQueryWrapper<SeckillProduct>().eq(SeckillProduct::getActivityId, id));

        for (SeckillActivityForm.SeckillProductForm pf : form.getProducts()) {
            SeckillProduct sp = new SeckillProduct();
            sp.setActivityId(id);
            sp.setProductId(pf.getProductId());
            sp.setSkuId(pf.getSkuId() != null ? pf.getSkuId() : 0L);
            sp.setSeckillPrice(pf.getSeckillPrice());
            sp.setSeckillStock(pf.getSeckillStock());
            sp.setRemainStock(pf.getSeckillStock());
            sp.setLimitPerUser(pf.getLimitPerUser() != null ? pf.getLimitPerUser() : 1);
            seckillProductMapper.insert(sp);
        }

        log.info("Seckill activity updated: id={}", id);
    }

    @Override
    @Transactional
    public void adminDelete(Long id) {
        seckillProductMapper.delete(
                new LambdaQueryWrapper<SeckillProduct>().eq(SeckillProduct::getActivityId, id));
        activityMapper.deleteById(id);
        log.info("Seckill activity deleted: id={}", id);
    }

    @Override
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseTimeoutOrders() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(15);
        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY)
                        .lt(Order::getCreateTime, timeout));

        for (Order order : timeoutOrders) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            for (OrderItem item : items) {
                // Restore seckill stock
                List<SeckillProduct> sps = seckillProductMapper.selectList(
                        new LambdaQueryWrapper<SeckillProduct>()
                                .eq(SeckillProduct::getProductId, item.getProductId()));
                for (SeckillProduct sp : sps) {
                    if (sp.getRemainStock() < sp.getSeckillStock()) {
                        seckillProductMapper.update(null,
                                new LambdaUpdateWrapper<SeckillProduct>()
                                        .eq(SeckillProduct::getId, sp.getId())
                                        .setSql("remain_stock = remain_stock + 1"));
                    }
                }
                // Restore product/SKU stock
                productMapper.update(null,
                        new LambdaUpdateWrapper<Product>()
                                .eq(Product::getId, item.getProductId())
                                .setSql("stock = stock + " + item.getQuantity()));
                if (item.getSkuId() != null && item.getSkuId() > 0) {
                    skuMapper.update(null,
                            new LambdaUpdateWrapper<ProductSku>()
                                    .eq(ProductSku::getId, item.getSkuId())
                                    .setSql("stock = stock + " + item.getQuantity()));
                }
            }
            // Release coupons
            if (order.getCouponIds() != null && !order.getCouponIds().isBlank()) {
                couponService.releaseCoupons(order.getCouponIds());
            }
            order.setStatus(OrderStatus.CANCELLED);
            orderMapper.updateById(order);
            log.info("Seckill order timeout cancelled: orderNo={}", order.getOrderNo());
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void syncActivityStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // Set status=1 for activities that have started and not yet ended
        activityMapper.update(null,
                new LambdaUpdateWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 0)
                        .le(SeckillActivity::getStartTime, now)
                        .gt(SeckillActivity::getEndTime, now)
                        .set(SeckillActivity::getStatus, 1));

        // Set status=2 for activities that have ended
        activityMapper.update(null,
                new LambdaUpdateWrapper<SeckillActivity>()
                        .ne(SeckillActivity::getStatus, 2)
                        .lt(SeckillActivity::getEndTime, now)
                        .set(SeckillActivity::getStatus, 2));
    }

    private void fillActivityProducts(SeckillActivity activity) {
        List<SeckillProduct> products = seckillProductMapper.selectList(
                new LambdaQueryWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getActivityId, activity.getId()));
        for (SeckillProduct sp : products) {
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
                    if (sku.getPrice() != null) {
                        sp.setOriginalPrice(sku.getPrice());
                    }
                }
            }
        }
        activity.setProducts(products);
    }

    private void validateNoTimeOverlap(LocalDateTime start, LocalDateTime end, Long excludeId) {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        if (excludeId != null) {
            wrapper.ne(SeckillActivity::getId, excludeId);
        }
        wrapper.lt(SeckillActivity::getStartTime, end)
                .gt(SeckillActivity::getEndTime, start);
        List<SeckillActivity> overlapping = activityMapper.selectList(wrapper);
        if (!overlapping.isEmpty()) {
            throw new BusinessException("当前时段已存在秒杀活动「" + overlapping.get(0).getName() + "」，同一时段仅允许一个活动");
        }
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", new Random().nextInt(1000000));
    }
}
