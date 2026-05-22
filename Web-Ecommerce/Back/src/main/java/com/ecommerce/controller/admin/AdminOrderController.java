package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.Order;
import com.ecommerce.service.OrderService;
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
