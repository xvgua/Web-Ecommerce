package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.dto.RefundAuditRequest;
import com.ecommerce.dto.RefundQuery;
import com.ecommerce.entity.Order;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public Result<PageResult<Order>> list(ProductQuery query) {
        return Result.success(orderService.adminGetOrderPage(query));
    }

    // ===== Refund Management =====

    @GetMapping("/refunds")
    public Result<PageResult<Order>> refundList(RefundQuery query) {
        return Result.success(orderService.adminGetRefundPage(query));
    }

    @GetMapping("/refunds/{orderId}")
    public Result<Order> refundDetail(@PathVariable Long orderId) {
        return Result.success(orderService.adminGetRefundDetail(orderId));
    }

    @PutMapping("/refunds/{orderId}/audit")
    public Result<Void> auditRefund(@PathVariable Long orderId, @Valid @RequestBody RefundAuditRequest req) {
        orderService.auditRefund(orderId, req);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.adminGetOrderById(id));
    }

    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.shipOrder(id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.adminCancelOrder(id);
        return Result.success();
    }
}
