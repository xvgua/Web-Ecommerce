package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderQuery;
import com.ecommerce.dto.PayIntentResponse;
import com.ecommerce.dto.PayStatusResponse;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.dto.RefundApplyRequest;
import com.ecommerce.dto.RefundAuditRequest;
import com.ecommerce.dto.RefundQuery;

import com.ecommerce.entity.Order;
import jakarta.servlet.http.HttpServletResponse;

public interface OrderService {
    Order createOrder(Long userId, CreateOrderRequest req);
    PageResult<Order> getOrderPage(Long userId, OrderQuery query);
    Order getOrderById(Long userId, Long id);
    void payOrder(Long userId, Long id, String payMethod);
    void cancelOrder(Long userId, Long id);
    void confirmReceive(Long userId, Long id);
    void updateAddress(Long userId, Long id, Long addressId);
    void refundOrder(Long userId, Long id, RefundApplyRequest req);
    Order getRefundDetail(Long userId, Long id);
    void cancelRefundApplication(Long userId, Long id);
    Order reorder(Long userId, Long id);

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

    // Admin export
    void exportOrders(ProductQuery query, HttpServletResponse response);

    // Admin refund
    PageResult<Order> adminGetRefundPage(RefundQuery query);
    Order adminGetRefundDetail(Long orderId);
    void auditRefund(Long orderId, RefundAuditRequest req);
}
