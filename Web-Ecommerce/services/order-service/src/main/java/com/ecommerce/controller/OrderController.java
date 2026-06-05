package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.AddressUpdateRequest;
import com.ecommerce.dto.OrderStats;
import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.CreatePayIntentRequest;
import com.ecommerce.dto.OrderQuery;
import com.ecommerce.dto.PayIntentResponse;
import com.ecommerce.dto.PayOrderRequest;
import com.ecommerce.dto.PayStatusResponse;
import com.ecommerce.dto.RefundApplyRequest;

import com.ecommerce.entity.Order;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/stats")
    public Result<OrderStats> stats() {
        return Result.success(orderService.getOrderStats(UserContext.getUserId()));
    }

    @GetMapping
    public Result<PageResult<Order>> list(OrderQuery query) {
        return Result.success(orderService.getOrderPage(UserContext.getUserId(), query));
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(UserContext.getUserId(), id));
    }

    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderRequest req) {
        return Result.success(orderService.createOrder(UserContext.getUserId(), req));
    }

    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestBody(required = false) PayOrderRequest req) {
        String payMethod = req != null ? req.getPayMethod() : null;
        orderService.payOrder(UserContext.getUserId(), id, payMethod);
        return Result.success();
    }

    @PostMapping("/{id}/pay-intent")
    public Result<PayIntentResponse> createPayIntent(@PathVariable Long id, @RequestBody CreatePayIntentRequest req) {
        return Result.success(orderService.createPayIntent(UserContext.getUserId(), id, req.getPayMethod()));
    }

    @GetMapping("/{id}/pay-status")
    public Result<PayStatusResponse> getPayStatus(@PathVariable Long id) {
        return Result.success(orderService.getPayStatus(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/scan-simulate")
    public Result<PayStatusResponse> simulateScan(@PathVariable Long id) {
        return Result.success(orderService.simulateScan(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}/pay/confirm")
    public Result<Void> confirmPay(@PathVariable Long id) {
        orderService.confirmPay(UserContext.getUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(UserContext.getUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id, @Valid @RequestBody RefundApplyRequest req) {
        orderService.refundOrder(UserContext.getUserId(), id, req);
        return Result.success();
    }

    @GetMapping("/{id}/refund")
    public Result<Order> refundDetail(@PathVariable Long id) {
        return Result.success(orderService.getRefundDetail(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}/refund/cancel")
    public Result<Void> cancelRefund(@PathVariable Long id) {
        orderService.cancelRefundApplication(UserContext.getUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/reorder")
    public Result<Order> reorder(@PathVariable Long id) {
        return Result.success(orderService.reorder(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceive(UserContext.getUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/address")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody AddressUpdateRequest req) {
        orderService.updateAddress(UserContext.getUserId(), id, req.getAddressId());
        return Result.success();
    }
}
