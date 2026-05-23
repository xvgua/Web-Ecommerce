package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderQuery;
import com.ecommerce.dto.PayOrderRequest;
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

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(UserContext.getUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceive(UserContext.getUserId(), id);
        return Result.success();
    }
}
