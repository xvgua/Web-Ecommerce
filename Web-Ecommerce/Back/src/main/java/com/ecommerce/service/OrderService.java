package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderQuery;
import com.ecommerce.dto.PayIntentResponse;
import com.ecommerce.dto.PayStatusResponse;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Order;

public interface OrderService {
    Order createOrder(Long userId, CreateOrderRequest req);
    PageResult<Order> getOrderPage(Long userId, OrderQuery query);
    Order getOrderById(Long userId, Long id);
    void payOrder(Long userId, Long id, String payMethod);
    void cancelOrder(Long userId, Long id);
    void confirmReceive(Long userId, Long id);

    // Payment flow
    PayIntentResponse createPayIntent(Long userId, Long id, String payMethod);
    PayStatusResponse getPayStatus(Long userId, Long id);
    PayStatusResponse simulateScan(Long userId, Long id);
    void confirmPay(Long userId, Long id);

    // Admin
    PageResult<Order> adminGetOrderPage(ProductQuery query);
    Order adminGetOrderById(Long id);
    void shipOrder(Long id);
    void adminCancelOrder(Long id);
}
