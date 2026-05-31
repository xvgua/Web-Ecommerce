package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderQuery;
import com.ecommerce.dto.PayIntentResponse;
import com.ecommerce.dto.PayStatusResponse;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.*;
import com.ecommerce.mapper.*;
import com.ecommerce.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private UserMapper userMapper;
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

        // Validate address
        Address address = addressMapper.selectById(req.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        // Generate order
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAddressId(req.getAddressId());
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setRemark(req.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null || product.getStatus() == ProductStatus.OFF_SHELF) {
                throw new BusinessException("商品「" + (product != null ? product.getName() : "未知") + "」已下架");
            }

            BigDecimal price = product.getPrice();
            String specDesc = "";
            int stock = product.getStock();

            if (cart.getSkuId() != null && cart.getSkuId() > 0) {
                ProductSku sku = skuMapper.selectById(cart.getSkuId());
                if (sku != null) {
                    price = sku.getPrice();
                    stock = sku.getStock();
                    specDesc = sku.getSpecName() + ":" + sku.getSpecValue();
                }
            }

            if (cart.getQuantity() > stock) {
                throw new BusinessException("商品「" + product.getName() + "」库存不足");
            }

            // Deduct stock (optimistic lock)
            if (cart.getSkuId() != null && cart.getSkuId() > 0) {
                int affected = skuMapper.update(null,
                        new LambdaUpdateWrapper<ProductSku>()
                                .eq(ProductSku::getId, cart.getSkuId())
                                .ge(ProductSku::getStock, cart.getQuantity())
                                .setSql("stock = stock - " + cart.getQuantity())
                                .setSql("sales = sales + " + cart.getQuantity()));
                if (affected == 0) {
                    throw new BusinessException("商品「" + product.getName() + "」库存不足");
                }
                // Also increment product-level sales
                productMapper.update(null,
                        new LambdaUpdateWrapper<Product>()
                                .eq(Product::getId, product.getId())
                                .setSql("sales = sales + " + cart.getQuantity()));
            } else {
                int affected = productMapper.update(null,
                        new LambdaUpdateWrapper<Product>()
                                .eq(Product::getId, product.getId())
                                .ge(Product::getStock, cart.getQuantity())
                                .setSql("stock = stock - " + cart.getQuantity())
                                .setSql("sales = sales + " + cart.getQuantity()));
                if (affected == 0) {
                    throw new BusinessException("商品「" + product.getName() + "」库存不足");
                }
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

        // Apply coupon if provided
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (req.getUserCouponId() != null) {
            couponDiscount = couponService.calculateDiscount(req.getUserCouponId(), totalAmount);
            order.setCouponId(req.getUserCouponId());
            order.setCouponDiscount(couponDiscount);
        }
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount.subtract(couponDiscount));
        orderMapper.insert(order);

        // Mark coupon as used
        if (req.getUserCouponId() != null) {
            couponService.markAsUsed(req.getUserCouponId(), order.getId());
        }

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // Clear selected cart items (skip for buy-now orders)
        if (req.getProductId() == null) {
            List<Long> cartIds = cartItems.stream().map(Cart::getId).toList();
            cartMapper.deleteBatchIds(cartIds);
        }

        log.info("Order created: orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, totalAmount);
        return order;
    }

    @Override
    public PageResult<Order> getOrderPage(Long userId, OrderQuery query) {
        // Review-filter mode: completed orders filtered by review completion
        if (query.getReviewFilter() != null) {
            return getOrderPageByReviewFilter(userId, query);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (query.getStatus() != null) {
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
        // Fetch all completed orders for this user
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(Order::getStatus, OrderStatus.COMPLETED);
        wrapper.orderByDesc(Order::getCreateTime);

        java.util.List<Order> allOrders = orderMapper.selectList(wrapper);
        for (Order order : allOrders) {
            fillOrderDetail(order);
        }

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        // Calculate review status for each order
        String filter = query.getReviewFilter();
        java.util.List<Order> filtered = new java.util.ArrayList<>();
        for (Order order : allOrders) {
            int itemCount = order.getItems() != null ? order.getItems().size() : 0;
            if (itemCount == 0) continue;

            // Orders completed more than 1 month ago: closed for review, only visible in "Completed" tab
            if (order.getDealTime() != null && order.getDealTime().isBefore(oneMonthAgo)) {
                continue;
            }

            // Count distinct products that have an initial review (not follow-up)
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

        // Manual pagination
        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        int offset = (page - 1) * pageSize;
        long total = filtered.size();

        java.util.List<Order> paged = filtered;
        if (offset < filtered.size()) {
            int toIndex = Math.min(offset + pageSize, filtered.size());
            paged = filtered.subList(offset, toIndex);
        } else {
            paged = java.util.List.of();
        }

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

        // Return existing session if still waiting for scan
        PaymentSession existing = paymentSessionMapper.selectOne(
                new LambdaQueryWrapper<PaymentSession>().eq(PaymentSession::getOrderId, id));
        if (existing != null && "WAITING_SCAN".equals(existing.getStatus())) {
            BigDecimal amount = order.getPayAmount() != null && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0
                    ? order.getPayAmount() : order.getTotalAmount();
            return new PayIntentResponse(existing.getQrToken(), order.getOrderNo(),
                    amount, existing.getPayMethod());
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
        // Conditional update to prevent concurrent double-confirm
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
        if (order.getCouponId() != null) {
            couponService.releaseCoupon(order.getCouponId());
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
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        order.setAddressId(addressId);
        order.setAddressModified(1);
        orderMapper.updateById(order);
        log.info("Order address updated: orderNo={}, newAddressId={}", order.getOrderNo(), addressId);
    }

    @Override
    @Transactional
    public void refundOrder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_SHIP && order.getStatus() != OrderStatus.SHIPPED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("当前订单状态不支持退款");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.REFUNDING);
        orderMapper.updateById(order);
        log.info("Order refund requested: orderNo={}", order.getOrderNo());
    }

    @Override
    @Transactional
    public Order reorder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException("该订单状态不支持此操作");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        if (items.isEmpty()) {
            throw new BusinessException("订单无商品");
        }

        // Check stock for all items
        boolean stockSufficient = true;
        for (OrderItem item : items) {
            int availableStock;
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                ProductSku sku = skuMapper.selectById(item.getSkuId());
                availableStock = sku != null ? sku.getStock() : 0;
            } else {
                Product product = productMapper.selectById(item.getProductId());
                availableStock = product != null ? product.getStock() : 0;
            }
            if (item.getQuantity() > availableStock) {
                stockSufficient = false;
                break;
            }
        }

        if (stockSufficient) {
            // Create order directly
            Order newOrder = new Order();
            newOrder.setOrderNo(generateOrderNo());
            newOrder.setUserId(userId);
            newOrder.setAddressId(order.getAddressId());
            newOrder.setStatus(OrderStatus.PENDING_PAY);
            newOrder.setRemark("");

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> newItems = new ArrayList<>();

            for (OrderItem oldItem : items) {
                BigDecimal price;
                if (oldItem.getSkuId() != null && oldItem.getSkuId() > 0) {
                    ProductSku sku = skuMapper.selectById(oldItem.getSkuId());
                    if (sku == null) throw new BusinessException("规格不存在，请重新下单");
                    int affected = skuMapper.update(null,
                            new LambdaUpdateWrapper<ProductSku>()
                                    .eq(ProductSku::getId, oldItem.getSkuId())
                                    .ge(ProductSku::getStock, oldItem.getQuantity())
                                    .setSql("stock = stock - " + oldItem.getQuantity())
                                    .setSql("sales = sales + " + oldItem.getQuantity()));
                    if (affected == 0) throw new BusinessException("商品「" + oldItem.getProductName() + "」库存不足");
                    // Also increment product-level sales
                    productMapper.update(null,
                            new LambdaUpdateWrapper<Product>()
                                    .eq(Product::getId, oldItem.getProductId())
                                    .setSql("sales = sales + " + oldItem.getQuantity()));
                    price = sku.getPrice();
                } else {
                    Product product = productMapper.selectById(oldItem.getProductId());
                    if (product == null || product.getStatus() == ProductStatus.OFF_SHELF)
                        throw new BusinessException("商品「" + oldItem.getProductName() + "」已下架");
                    int affected = productMapper.update(null,
                            new LambdaUpdateWrapper<Product>()
                                    .eq(Product::getId, oldItem.getProductId())
                                    .ge(Product::getStock, oldItem.getQuantity())
                                    .setSql("stock = stock - " + oldItem.getQuantity())
                                    .setSql("sales = sales + " + oldItem.getQuantity()));
                    if (affected == 0) throw new BusinessException("商品「" + oldItem.getProductName() + "」库存不足");
                    price = product.getPrice();
                }

                OrderItem newItem = new OrderItem();
                newItem.setProductId(oldItem.getProductId());
                newItem.setProductName(oldItem.getProductName());
                newItem.setProductImage(oldItem.getProductImage());
                newItem.setSkuId(oldItem.getSkuId());
                newItem.setSpecDesc(oldItem.getSpecDesc());
                newItem.setQuantity(oldItem.getQuantity());
                newItem.setPrice(price);
                newItems.add(newItem);
                totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(oldItem.getQuantity())));
            }

            newOrder.setTotalAmount(totalAmount);
            orderMapper.insert(newOrder);
            for (OrderItem ni : newItems) {
                ni.setOrderId(newOrder.getId());
                orderItemMapper.insert(ni);
            }
            log.info("Reordered directly: orderNo={}, userId={}, newOrderNo={}",
                    order.getOrderNo(), userId, newOrder.getOrderNo());
            return newOrder;
        }

        // Stock insufficient, add to cart
        for (OrderItem item : items) {
            Long skuId = item.getSkuId() != null ? item.getSkuId() : 0L;
            Cart existing = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, userId)
                    .eq(Cart::getProductId, item.getProductId())
                    .eq(Cart::getSkuId, skuId));
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                cartMapper.updateById(existing);
            } else {
                Cart cart = new Cart();
                cart.setUserId(userId);
                cart.setProductId(item.getProductId());
                cart.setSkuId(skuId);
                cart.setQuantity(item.getQuantity());
                cart.setChecked(1);
                cartMapper.insert(cart);
            }
        }
        log.info("Reordered to cart (stock insufficient): orderNo={}, userId={}, items={}",
                order.getOrderNo(), userId, items.size());
        return null;
    }

    @Override
    public void confirmReceive(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException("订单状态不正确");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setDealTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order confirmed: orderNo={}", order.getOrderNo());
    }

    // Admin methods
    @Override
    public PageResult<Order> adminGetOrderPage(ProductQuery query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(Order::getStatus, query.getStatus());
        }
        if (query.getUserId() != null) {
            wrapper.eq(Order::getUserId, query.getUserId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.eq(Order::getOrderNo, query.getKeyword());
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);

        for (Order order : result.getRecords()) {
            fillOrderDetail(order);
        }

        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public Order adminGetOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        fillOrderDetail(order);
        return order;
    }

    @Override
    public void shipOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (order.getStatus() != OrderStatus.PENDING_SHIP) {
            throw new BusinessException("只能对待发货订单发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        orderMapper.updateById(order);
        log.info("Order shipped: orderNo={}", order.getOrderNo());
    }

    @Override
    public void adminCancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException("订单状态不允许取消");
        }
        if (order.getStatus() == OrderStatus.PENDING_SHIP || order.getStatus() == OrderStatus.SHIPPED) {
            restoreStock(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        log.info("Order cancelled by admin: orderNo={}", order.getOrderNo());
    }

    private void fillOrderDetail(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        // Mark items that have been reviewed and whether a follow-up exists
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId()));
        Set<Long> reviewedProductIds = reviews.stream()
                .filter(r -> r.getIsFollowup() == 0)
                .map(Review::getProductId)
                .collect(Collectors.toSet());
        Set<Long> followUpProductIds = reviews.stream()
                .filter(r -> r.getIsFollowup() == 1)
                .map(Review::getProductId)
                .collect(Collectors.toSet());
        for (OrderItem item : items) {
            item.setReviewed(reviewedProductIds.contains(item.getProductId()));
            item.setHasFollowUp(followUpProductIds.contains(item.getProductId()));
        }

        order.setItems(items);

        // Populate coupon name if a coupon was used
        if (order.getCouponId() != null) {
            UserCoupon uc = userCouponMapper.selectById(order.getCouponId());
            if (uc != null) {
                Coupon coupon = couponMapper.selectById(uc.getCouponId());
                if (coupon != null) {
                    order.setCouponName(coupon.getName());
                }
            }
        }

        Address address = addressMapper.selectById(order.getAddressId());
        order.setAddress(address);

        order.setStatusText(getStatusText(order.getStatus()));
    }

    private void restoreStock(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
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
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", new Random().nextInt(1000000));
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "退款中";
            default -> "未知";
        };
    }
}
