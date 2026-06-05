package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.feign.ProductFeignClient;
import com.ecommerce.feign.UserFeignClient;
import com.ecommerce.mapper.*;
import com.ecommerce.service.CouponService;
import com.ecommerce.service.OrderService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private PaymentSessionMapper paymentSessionMapper;
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Order createOrder(Long userId, CreateOrderRequest req) {
        List<Cart> cartItems;

        if (req.getProductId() != null) {
            if (req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new BusinessException("请选择购买数量");
            }
            Cart cart = new Cart();
            cart.setId(0L);
            cart.setUserId(userId);
            cart.setProductId(req.getProductId());
            cart.setSkuId(req.getSkuId() != null ? req.getSkuId() : 0L);
            cart.setQuantity(req.getQuantity());
            cart.setChecked(1);
            cartItems = Collections.singletonList(cart);
        } else {
            if (req.getCartItemIds() == null || req.getCartItemIds().isEmpty()) {
                throw new BusinessException("请选择要购买的商品");
            }
            cartItems = cartMapper.selectList(
                    new LambdaQueryWrapper<Cart>()
                            .eq(Cart::getUserId, userId)
                            .eq(Cart::getChecked, 1)
                            .in(Cart::getId, req.getCartItemIds()));
            if (cartItems.isEmpty()) {
                throw new BusinessException("请选择要购买的商品");
            }
        }

        // Validate address via Feign
        Result<Address> addrResult = userFeignClient.getAddressById(req.getAddressId());
        if (!addrResult.isSuccess() || addrResult.getData() == null) {
            throw new BusinessException("收货地址不存在");
        }
        Address address = addrResult.getData();
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAddressId(req.getAddressId());
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setRemark(req.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (Cart cart : cartItems) {
            // Get product via Feign
            Result<Product> prodResult = productFeignClient.getProductById(cart.getProductId());
            if (!prodResult.isSuccess() || prodResult.getData() == null) {
                throw new BusinessException("商品不存在或已下架");
            }
            Product product = prodResult.getData();
            if (product.getStatus() == ProductStatus.OFF_SHELF) {
                throw new BusinessException("商品「" + product.getName() + "」已下架");
            }

            BigDecimal price = product.getPrice();
            String specDesc = "";
            int stock = product.getStock();

            if (cart.getSkuId() != null && cart.getSkuId() > 0) {
                Result<ProductSku> skuResult = productFeignClient.getSkuById(cart.getSkuId());
                if (skuResult.isSuccess() && skuResult.getData() != null) {
                    ProductSku sku = skuResult.getData();
                    price = sku.getPrice();
                    stock = sku.getStock();
                    specDesc = sku.getSpecName() + ":" + sku.getSpecValue();
                }
            }

            if (cart.getQuantity() > stock) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足");
            }

            // Deduct stock via Feign
            Result<Void> stockResult = productFeignClient.deductStock(
                    new DeductStockRequest(product.getId(), cart.getSkuId(), cart.getQuantity()));
            if (!stockResult.isSuccess()) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足");
            }

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setSkuId(cart.getSkuId());
            item.setSpecDesc(specDesc);
            item.setQuantity(cart.getQuantity());
            item.setPrice(price);
            items.add(item);

            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        order.setTotalAmount(totalAmount);

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (req.getUserCouponIds() != null && !req.getUserCouponIds().isEmpty()) {
            couponDiscount = couponService.calculateTotalDiscount(req.getUserCouponIds(), totalAmount);
            order.setCouponIds(req.getUserCouponIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
            order.setCouponDiscount(couponDiscount);
        }
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount.subtract(couponDiscount));
        orderMapper.insert(order);

        if (req.getUserCouponIds() != null && !req.getUserCouponIds().isEmpty()) {
            couponService.markAsUsed(req.getUserCouponIds(), order.getId());
        }

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        if (req.getProductId() == null) {
            List<Long> cartIds = cartItems.stream().map(Cart::getId).toList();
            cartMapper.deleteBatchIds(cartIds);
        }

        log.info("Order created: orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, totalAmount);
        return order;
    }

    @Override
    public PageResult<Order> getOrderPage(Long userId, OrderQuery query) {
        if (query.getReviewFilter() != null) {
            return getOrderPageByReviewFilter(userId, query);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (Boolean.TRUE.equals(query.getHasRefund())) {
            wrapper.isNotNull(Order::getRefundStatus);
        } else if (query.getStatus() != null) {
            wrapper.eq(Order::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);

        for (Order order : result.getRecords()) {
            fillOrderDetail(order);
        }

        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    private PageResult<Order> getOrderPageByReviewFilter(Long userId, OrderQuery query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(Order::getStatus, OrderStatus.COMPLETED);
        wrapper.orderByDesc(Order::getCreateTime);

        List<Order> allOrders = orderMapper.selectList(wrapper);
        for (Order order : allOrders) {
            fillOrderDetail(order);
        }

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        String filter = query.getReviewFilter();
        List<Order> filtered = new ArrayList<>();
        for (Order order : allOrders) {
            int itemCount = order.getItems() != null ? order.getItems().size() : 0;
            if (itemCount == 0) continue;
            if (order.getDealTime() != null && order.getDealTime().isBefore(oneMonthAgo)) continue;

            List<Review> reviews = reviewMapper.selectList(
                    new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId()));
            long reviewedItemCount = reviews.stream()
                    .filter(r -> r.getIsFollowup() == 0)
                    .map(Review::getProductId)
                    .distinct()
                    .count();
            order.setReviewCount(reviewedItemCount);

            if ("pending".equals(filter) && reviewedItemCount < itemCount) {
                filtered.add(order);
            } else if ("reviewed".equals(filter) && reviewedItemCount >= itemCount) {
                filtered.add(order);
            }
        }

        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        int offset = (page - 1) * pageSize;
        long total = filtered.size();
        List<Order> paged = offset < filtered.size()
                ? filtered.subList(offset, Math.min(offset + pageSize, filtered.size()))
                : List.of();

        return PageResult.of(paged, total, page, pageSize);
    }

    @Override
    public Order getOrderById(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        fillOrderDetail(order);
        return order;
    }

    @Override
    public void payOrder(Long userId, Long id, String payMethod) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus(OrderStatus.PENDING_SHIP);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order paid: orderNo={}, payMethod={}", order.getOrderNo(), payMethod);
    }

    @Override
    public PayIntentResponse createPayIntent(Long userId, Long id, String payMethod) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BusinessException("订单状态不正确");
        }
        if (payMethod == null || payMethod.isBlank()) {
            throw new BusinessException("请选择支付方式");
        }
        if (!List.of("wechat", "alipay", "card").contains(payMethod)) {
            throw new BusinessException("不支持的支付方式");
        }

        PaymentSession existing = paymentSessionMapper.selectOne(
                new LambdaQueryWrapper<PaymentSession>().eq(PaymentSession::getOrderId, id));
        if (existing != null && "WAITING_SCAN".equals(existing.getStatus())) {
            BigDecimal amount = order.getPayAmount() != null && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0
                    ? order.getPayAmount() : order.getTotalAmount();
            return new PayIntentResponse(existing.getQrToken(), order.getOrderNo(), amount, existing.getPayMethod());
        }

        String qrToken = UUID.randomUUID().toString().replace("-", "");
        PaymentSession session = new PaymentSession();
        session.setOrderId(id);
        session.setPayMethod(payMethod);
        session.setQrToken(qrToken);
        session.setQrScanned(0);
        session.setStatus("WAITING_SCAN");
        paymentSessionMapper.insert(session);

        BigDecimal amount = order.getPayAmount() != null && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0
                ? order.getPayAmount() : order.getTotalAmount();
        log.info("Pay intent created: orderNo={}, payMethod={}", order.getOrderNo(), payMethod);
        return new PayIntentResponse(qrToken, order.getOrderNo(), amount, payMethod);
    }

    @Override
    public PayStatusResponse getPayStatus(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        PaymentSession session = paymentSessionMapper.selectOne(
                new LambdaQueryWrapper<PaymentSession>().eq(PaymentSession::getOrderId, id));
        if (session == null) {
            return new PayStatusResponse("NONE", false, null);
        }
        return new PayStatusResponse(session.getStatus(), session.getQrScanned() == 1, session.getPayMethod());
    }

    @Override
    public PayStatusResponse simulateScan(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        PaymentSession session = paymentSessionMapper.selectOne(
                new LambdaQueryWrapper<PaymentSession>().eq(PaymentSession::getOrderId, id));
        if (session == null) {
            throw new BusinessException("未找到支付会话");
        }
        if (!"WAITING_SCAN".equals(session.getStatus())) {
            throw new BusinessException("当前状态不允许扫码");
        }
        session.setQrScanned(1);
        session.setScanTime(LocalDateTime.now());
        session.setStatus("SCANNED");
        paymentSessionMapper.updateById(session);
        log.info("QR scanned: orderNo={}", order.getOrderNo());
        return new PayStatusResponse("SCANNED", true, session.getPayMethod());
    }

    @Override
    @Transactional
    @CacheEvict(value = "hotProducts", allEntries = true)
    public void confirmPay(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BusinessException("订单状态不正确");
        }
        PaymentSession session = paymentSessionMapper.selectOne(
                new LambdaQueryWrapper<PaymentSession>().eq(PaymentSession::getOrderId, id));
        if (session == null || session.getQrScanned() != 1) {
            throw new BusinessException("请先扫描二维码");
        }
        order.setStatus(OrderStatus.PENDING_SHIP);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        int affected = paymentSessionMapper.update(null,
                new LambdaUpdateWrapper<PaymentSession>()
                        .eq(PaymentSession::getOrderId, id)
                        .eq(PaymentSession::getQrScanned, 1)
                        .eq(PaymentSession::getStatus, "SCANNED")
                        .set(PaymentSession::getStatus, "PAID"));
        if (affected == 0) {
            throw new BusinessException("支付确认失败，请重试");
        }
        log.info("Payment confirmed: orderNo={}, payMethod={}", order.getOrderNo(), session.getPayMethod());
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BusinessException("只能取消待支付订单");
        }
        restoreStock(order);
        if (order.getCouponIds() != null && !order.getCouponIds().isBlank()) {
            couponService.releaseCoupons(order.getCouponIds());
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        log.info("Order cancelled: orderNo={}", order.getOrderNo());
    }

    @Override
    public void updateAddress(Long userId, Long id, Long addressId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_SHIP) {
            throw new BusinessException("仅待发货订单可修改地址");
        }
        if (order.getAddressModified() != null && order.getAddressModified() == 1) {
            throw new BusinessException("您已经修改过地址啦");
        }
        Result<Address> addrResult = userFeignClient.getAddressById(addressId);
        if (!addrResult.isSuccess() || addrResult.getData() == null) {
            throw new BusinessException("地址不存在");
        }
        if (!addrResult.getData().getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        order.setAddressId(addressId);
        order.setAddressModified(1);
        orderMapper.updateById(order);
        log.info("Order address updated: orderNo={}, newAddressId={}", order.getOrderNo(), addressId);
    }

    // --- refund methods unchanged from original monolith ---
    @Override
    @Transactional
    public void refundOrder(Long userId, Long id, RefundApplyRequest req) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_SHIP && order.getStatus() != OrderStatus.SHIPPED
                && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("当前订单状态不支持退款");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() != RefundStatus.REJECTED
                && order.getRefundStatus() != RefundStatus.CANCELLED) {
            throw new BusinessException("该订单已有退款申请在处理中");
        }
        if (req.getRefundType() == 2 && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("仅已收货订单支持退货退款");
        }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        Set<Long> allItemIds = allItems.stream().map(OrderItem::getId).collect(Collectors.toSet());
        for (Long itemId : req.getItemIds()) {
            if (!allItemIds.contains(itemId)) throw new BusinessException("退款商品不属于该订单");
        }
        List<OrderItem> refundItems = allItems.stream()
                .filter(it -> req.getItemIds().contains(it.getId())).toList();
        BigDecimal refundAmount = refundItems.stream()
                .map(it -> it.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxRefund = order.getPayAmount() != null ? order.getPayAmount() : order.getTotalAmount();
        if (refundAmount.compareTo(maxRefund) > 0) refundAmount = maxRefund;

        order.setRefundType(req.getRefundType());
        order.setRefundReason(req.getRefundReason());
        order.setRefundDesc(req.getRefundDesc());
        order.setRefundAmount(refundAmount);
        order.setRefundApplyTime(LocalDateTime.now());
        try { order.setRefundItemIds(objectMapper.writeValueAsString(req.getItemIds())); }
        catch (Exception e) { order.setRefundItemIds(req.getItemIds().toString()); }

        if (order.getStatus() == OrderStatus.PENDING_SHIP) {
            order.setRefundStatus(RefundStatus.COMPLETED);
            order.setStatus(OrderStatus.REFUNDED);
            restoreStockForItems(order, req.getItemIds());
            log.info("Refund auto-approved: orderNo={}", order.getOrderNo());
        } else {
            order.setRefundStatus(RefundStatus.PENDING_REVIEW);
            log.info("Refund application submitted: orderNo={}", order.getOrderNo());
        }
        orderMapper.updateById(order);
    }

    @Override public Order getRefundDetail(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException(404, "订单不存在");
        if (order.getRefundStatus() == null) throw new BusinessException("该订单没有退款记录");
        fillOrderDetail(order); fillRefundDetail(order); return order;
    }
    @Override public void cancelRefundApplication(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException(404, "订单不存在");
        if (order.getRefundStatus() == null || order.getRefundStatus() != RefundStatus.PENDING_REVIEW)
            throw new BusinessException("当前退款状态不可撤销");
        order.setRefundStatus(RefundStatus.CANCELLED); orderMapper.updateById(order);
        log.info("Refund application cancelled: orderNo={}", order.getOrderNo());
    }
    @Override public PageResult<Order> adminGetRefundPage(RefundQuery query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Order::getRefundStatus);
        if (query.getRefundStatus() != null) wrapper.eq(Order::getRefundStatus, query.getRefundStatus());
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) wrapper.eq(Order::getOrderNo, query.getKeyword());
        wrapper.orderByDesc(Order::getRefundApplyTime);
        Page<Order> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        for (Order order : result.getRecords()) { fillOrderDetail(order); fillRefundDetail(order); }
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }
    @Override public Order adminGetRefundDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (order.getRefundStatus() == null) throw new BusinessException("该订单没有退款记录");
        fillOrderDetail(order); fillRefundDetail(order); return order;
    }
    @Override @Transactional
    public void auditRefund(Long orderId, RefundAuditRequest req) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (order.getRefundStatus() == null || order.getRefundStatus() != RefundStatus.PENDING_REVIEW)
            throw new BusinessException("当前退款状态不支持审核");
        order.setRefundDealTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(req.getApproved())) {
            order.setRefundStatus(RefundStatus.COMPLETED); order.setStatus(OrderStatus.REFUNDED);
            if (order.getRefundItemIds() != null && !order.getRefundItemIds().isBlank())
                restoreStockForItems(order, parseRefundItemIds(order.getRefundItemIds()));
            log.info("Refund approved: orderNo={}", order.getOrderNo());
        } else {
            if (req.getRejectReason() == null || req.getRejectReason().isBlank())
                throw new BusinessException("拒绝退款需填写原因");
            order.setRefundStatus(RefundStatus.REJECTED);
            order.setRefundRejectReason(req.getRejectReason());
            log.info("Refund rejected: orderNo={}", order.getOrderNo());
        }
        orderMapper.updateById(order);
    }

    @Override @Transactional
    public Order reorder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException(404, "订单不存在");
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.CANCELLED)
            throw new BusinessException("该订单状态不支持此操作");
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        if (items.isEmpty()) throw new BusinessException("订单无商品");

        boolean stockSufficient = true;
        for (OrderItem item : items) {
            int availableStock;
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                Result<ProductSku> sr = productFeignClient.getSkuById(item.getSkuId());
                availableStock = (sr.isSuccess() && sr.getData() != null) ? sr.getData().getStock() : 0;
            } else {
                Result<Product> pr = productFeignClient.getProductById(item.getProductId());
                availableStock = (pr.isSuccess() && pr.getData() != null) ? pr.getData().getStock() : 0;
            }
            if (item.getQuantity() > availableStock) { stockSufficient = false; break; }
        }

        if (stockSufficient) {
            Order newOrder = new Order();
            newOrder.setOrderNo(generateOrderNo()); newOrder.setUserId(userId);
            newOrder.setAddressId(order.getAddressId()); newOrder.setStatus(OrderStatus.PENDING_PAY);
            newOrder.setRemark(""); BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> newItems = new ArrayList<>();
            for (OrderItem oldItem : items) {
                BigDecimal price;
                if (oldItem.getSkuId() != null && oldItem.getSkuId() > 0) {
                    Result<ProductSku> sr = productFeignClient.getSkuById(oldItem.getSkuId());
                    if (!sr.isSuccess() || sr.getData() == null)
                        throw new BusinessException("规格不存在，请重新下单");
                    price = sr.getData().getPrice();
                    productFeignClient.deductStock(new DeductStockRequest(oldItem.getProductId(), oldItem.getSkuId(), oldItem.getQuantity()));
                } else {
                    Result<Product> pr = productFeignClient.getProductById(oldItem.getProductId());
                    if (!pr.isSuccess() || pr.getData() == null
                            || pr.getData().getStatus() == ProductStatus.OFF_SHELF)
                        throw new BusinessException("商品已下架");
                    price = pr.getData().getPrice();
                    productFeignClient.deductStock(new DeductStockRequest(oldItem.getProductId(), 0L, oldItem.getQuantity()));
                }
                OrderItem newItem = new OrderItem();
                newItem.setProductId(oldItem.getProductId()); newItem.setProductName(oldItem.getProductName());
                newItem.setProductImage(oldItem.getProductImage()); newItem.setSkuId(oldItem.getSkuId());
                newItem.setSpecDesc(oldItem.getSpecDesc()); newItem.setQuantity(oldItem.getQuantity());
                newItem.setPrice(price); newItems.add(newItem);
                totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(oldItem.getQuantity())));
            }
            newOrder.setTotalAmount(totalAmount); orderMapper.insert(newOrder);
            for (OrderItem ni : newItems) { ni.setOrderId(newOrder.getId()); orderItemMapper.insert(ni); }
            log.info("Reordered directly: orderNo={}, newOrderNo={}", order.getOrderNo(), newOrder.getOrderNo());
            return newOrder;
        }

        for (OrderItem item : items) {
            Long skuId = item.getSkuId() != null ? item.getSkuId() : 0L;
            Cart existing = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, userId).eq(Cart::getProductId, item.getProductId()).eq(Cart::getSkuId, skuId));
            if (existing != null) { existing.setQuantity(existing.getQuantity() + item.getQuantity()); cartMapper.updateById(existing); }
            else { Cart cart = new Cart(); cart.setUserId(userId); cart.setProductId(item.getProductId());
                   cart.setSkuId(skuId); cart.setQuantity(item.getQuantity()); cart.setChecked(1); cartMapper.insert(cart); }
        }
        log.info("Reordered to cart: orderNo={}, userId={}", order.getOrderNo(), userId);
        return null;
    }

    @Override public void confirmReceive(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException(404, "订单不存在");
        if (order.getStatus() != OrderStatus.SHIPPED) throw new BusinessException("订单状态不正确");
        order.setStatus(OrderStatus.COMPLETED); order.setDealTime(LocalDateTime.now()); orderMapper.updateById(order);
        log.info("Order confirmed: orderNo={}", order.getOrderNo());
    }

    @Override
    public void exportOrders(ProductQuery query, HttpServletResponse response) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) wrapper.eq(Order::getStatus, query.getStatus());
        if (query.getUserId() != null) wrapper.eq(Order::getUserId, query.getUserId());
        if (StringUtils.hasText(query.getKeyword())) wrapper.eq(Order::getOrderNo, query.getKeyword());
        wrapper.orderByDesc(Order::getCreateTime);
        long count = orderMapper.selectCount(wrapper);
        if (count > 10000) throw new BusinessException("匹配订单超过 10000 条上限（当前 " + count + " 条），请缩小筛选范围");
        if (count == 0) { writeEmptyExcel(response); return; }

        List<Order> orders = orderMapper.selectList(wrapper);
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Set<Long> addressIds = orders.stream().map(Order::getAddressId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItem>> itemsMap = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, Address> addressMap = new HashMap<>();
        if (!addressIds.isEmpty()) {
            Result<List<Address>> ar = userFeignClient.batchGetAddresses(new ArrayList<>(addressIds));
            if (ar.isSuccess() && ar.getData() != null)
                addressMap = ar.getData().stream().collect(Collectors.toMap(Address::getId, a -> a));
        }
        for (Order order : orders) {
            order.setItems(itemsMap.getOrDefault(order.getId(), List.of()));
            order.setAddress(addressMap.get(order.getAddressId()));
            order.setStatusText(getStatusText(order.getStatus()));
            if (order.getRefundStatus() != null) {
                order.setRefundReasonText(RefundReason.getReasonText(order.getRefundReason()));
                order.setRefundStatusText(RefundStatus.getStatusText(order.getRefundStatus()));
            }
        }
        List<OrderExcelDTO> sheet1 = orders.stream().map(this::toOrderExcelDTO).toList();
        List<OrderItemExcelDTO> sheet2 = orders.stream()
                .flatMap(o -> o.getItems().stream().map(item -> toItemExcelDTO(o.getOrderNo(), item))).toList();
        try {
            String filename = "订单导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            ExcelWriter ew = EasyExcel.write(response.getOutputStream()).build();
            ew.write(sheet1, EasyExcel.writerSheet(0, "订单列表").head(OrderExcelDTO.class).build());
            ew.write(sheet2, EasyExcel.writerSheet(1, "订单明细").head(OrderItemExcelDTO.class).build());
            ew.finish();
            log.info("Orders exported: count={}", orders.size());
        } catch (IOException e) { throw new BusinessException("导出Excel失败: " + e.getMessage()); }
    }

    @Override public PageResult<Order> adminGetOrderPage(ProductQuery query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) wrapper.eq(Order::getStatus, query.getStatus());
        if (query.getUserId() != null) wrapper.eq(Order::getUserId, query.getUserId());
        if (StringUtils.hasText(query.getKeyword())) wrapper.eq(Order::getOrderNo, query.getKeyword());
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        for (Order order : result.getRecords()) fillOrderDetail(order);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }
    @Override public Order adminGetOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        fillOrderDetail(order); return order;
    }
    @Override public void shipOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (order.getStatus() != OrderStatus.PENDING_SHIP) throw new BusinessException("只能对待发货订单发货");
        order.setStatus(OrderStatus.SHIPPED); orderMapper.updateById(order);
        log.info("Order shipped: orderNo={}", order.getOrderNo());
    }
    @Override public void adminCancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED)
            throw new BusinessException("订单状态不允许取消");
        if (order.getStatus() == OrderStatus.PENDING_SHIP || order.getStatus() == OrderStatus.SHIPPED)
            restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED); orderMapper.updateById(order);
        log.info("Order cancelled by admin: orderNo={}", order.getOrderNo());
    }

    // --- private helpers ---
    private void fillOrderDetail(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId()));
        Set<Long> reviewedIds = reviews.stream().filter(r -> r.getIsFollowup() == 0).map(Review::getProductId).collect(Collectors.toSet());
        Set<Long> followUpIds = reviews.stream().filter(r -> r.getIsFollowup() == 1).map(Review::getProductId).collect(Collectors.toSet());
        for (OrderItem item : items) {
            item.setReviewed(reviewedIds.contains(item.getProductId()));
            item.setHasFollowUp(followUpIds.contains(item.getProductId()));
        }
        order.setItems(items);
        if (order.getCouponIds() != null && !order.getCouponIds().isBlank()) {
            List<String> names = new ArrayList<>();
            for (String idStr : order.getCouponIds().split(",")) {
                try {
                    UserCoupon uc = userCouponMapper.selectById(Long.parseLong(idStr.trim()));
                    if (uc != null) { Coupon c = couponMapper.selectById(uc.getCouponId());
                    if (c != null) names.add(c.getName()); }
                } catch (NumberFormatException ignored) {}
            }
            order.setCouponName(String.join("、", names));
        }
        Result<Address> ar = userFeignClient.getAddressById(order.getAddressId());
        if (ar.isSuccess() && ar.getData() != null) order.setAddress(ar.getData());
        order.setStatusText(getStatusText(order.getStatus()));
        if (order.getRefundStatus() != null) {
            order.setRefundReasonText(RefundReason.getReasonText(order.getRefundReason()));
            order.setRefundStatusText(RefundStatus.getStatusText(order.getRefundStatus()));
        }
    }

    private void restoreStock(Order order) {
        orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()))
                .forEach(this::restoreStockForItem);
    }
    private void restoreStockForItems(Order order, List<Long> itemIds) {
        orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()))
                .stream().filter(it -> itemIds.contains(it.getId())).forEach(this::restoreStockForItem);
    }
    private void restoreStockForItem(OrderItem item) {
        productFeignClient.restoreStock(new RestoreStockRequest(item.getProductId(), item.getSkuId(), item.getQuantity()));
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", new Random().nextInt(1000000));
    }
    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待支付"; case 1 -> "待发货"; case 2 -> "待收货";
            case 3 -> "已完成"; case 4 -> "已取消"; case 5 -> "退款中"; case 6 -> "已退款";
            default -> "未知";
        };
    }
    private void fillRefundDetail(Order order) {
        order.setRefundReasonText(RefundReason.getReasonText(order.getRefundReason()));
        order.setRefundStatusText(RefundStatus.getStatusText(order.getRefundStatus()));
        if (order.getRefundItemIds() != null && !order.getRefundItemIds().isBlank()) {
            List<OrderItem> all = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            order.setRefundItems(all.stream().filter(it -> parseRefundItemIds(order.getRefundItemIds()).contains(it.getId())).toList());
        }
    }
    private List<Long> parseRefundItemIds(String s) {
        try { return objectMapper.readValue(s, new TypeReference<List<Long>>() {}); }
        catch (Exception e) {
            try { return Arrays.stream(s.replaceAll("[\\[\\]\\s]", "").split(","))
                    .filter(x -> !x.isEmpty()).map(Long::parseLong).collect(Collectors.toList()); }
            catch (Exception ex) { return List.of(); }
        }
    }
    private OrderExcelDTO toOrderExcelDTO(Order o) {
        OrderExcelDTO d = new OrderExcelDTO();
        d.setOrderNo(o.getOrderNo()); d.setUserId(o.getUserId());
        if (o.getAddress() instanceof Address a) {
            d.setReceiverName(a.getName()); d.setReceiverPhone(a.getPhone());
            d.setReceiverAddress(a.getProvince() + a.getCity() + a.getDistrict() + " " + a.getDetail());
        }
        d.setTotalAmount(o.getTotalAmount()); d.setCouponDiscount(o.getCouponDiscount());
        d.setDiscountAmount(o.getDiscountAmount()); d.setPayAmount(o.getPayAmount());
        d.setStatusText(o.getStatusText());
        d.setCreateTime(fdt(o.getCreateTime())); d.setPayTime(fdt(o.getPayTime())); d.setDealTime(fdt(o.getDealTime()));
        if (o.getRefundStatus() != null) {
            d.setRefundTypeText(o.getRefundType() != null && o.getRefundType() == 2 ? "退货退款" : "仅退款");
            d.setRefundAmount(o.getRefundAmount()); d.setRefundReasonText(o.getRefundReasonText());
            d.setRefundStatusText(o.getRefundStatusText());
            d.setRefundApplyTime(fdt(o.getRefundApplyTime())); d.setRefundDealTime(fdt(o.getRefundDealTime()));
        }
        return d;
    }
    private OrderItemExcelDTO toItemExcelDTO(String orderNo, OrderItem item) {
        OrderItemExcelDTO d = new OrderItemExcelDTO();
        d.setOrderNo(orderNo); d.setProductName(item.getProductName());
        d.setSpecDesc(item.getSpecDesc()); d.setPrice(item.getPrice()); d.setQuantity(item.getQuantity());
        d.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return d;
    }
    private String fdt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
    private void writeEmptyExcel(HttpServletResponse response) {
        try {
            String fn = "订单导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fn, StandardCharsets.UTF_8));
            ExcelWriter ew = EasyExcel.write(response.getOutputStream()).build();
            ew.write(List.of(), EasyExcel.writerSheet(0, "订单列表").head(OrderExcelDTO.class).build());
            ew.write(List.of(), EasyExcel.writerSheet(1, "订单明细").head(OrderItemExcelDTO.class).build());
            ew.finish();
        } catch (IOException e) { throw new BusinessException("导出Excel失败: " + e.getMessage()); }
    }
}
