package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateOrderRequest;
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

    @Override
    @Transactional
    public Order createOrder(Long userId, CreateOrderRequest req) {
        // Get selected cart items
        List<Cart> cartItems = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getChecked, 1)
                        .in(Cart::getId, req.getCartItemIds()));

        if (cartItems.isEmpty()) {
            throw new BusinessException("请选择要购买的商品");
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
                                .setSql("stock = stock - " + cart.getQuantity()));
                if (affected == 0) {
                    throw new BusinessException("商品「" + product.getName() + "」库存不足");
                }
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
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // Clear selected cart items
        List<Long> cartIds = cartItems.stream().map(Cart::getId).toList();
        cartMapper.deleteBatchIds(cartIds);

        log.info("Order created: orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, totalAmount);
        return order;
    }

    @Override
    public PageResult<Order> getOrderPage(Long userId, ProductQuery query) {
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
    public void payOrder(Long userId, Long id) {
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
        log.info("Order paid: orderNo={}", order.getOrderNo());
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
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        log.info("Order cancelled: orderNo={}", order.getOrderNo());
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
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.eq(Order::getOrderNo, query.getKeyword())
                    .or().eq(Order::getUserId, query.getKeyword()));
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
        order.setItems(items);

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
