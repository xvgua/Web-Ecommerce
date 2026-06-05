package com.ecommerce.controller.admin;

import com.ecommerce.common.Result;
import com.ecommerce.dto.HotProductDTO;
import com.ecommerce.dto.SalesTrendDTO;
import com.ecommerce.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(dashboardService.getDashboardStats());
    }

    @GetMapping("/sales-trend")
    public Result<List<SalesTrendDTO>> salesTrend(@RequestParam(defaultValue = "7d") String range) {
        return Result.success(dashboardService.getSalesTrend(range));
    }

    @GetMapping("/hot-products")
    public Result<List<HotProductDTO>> hotProducts(@RequestParam(defaultValue = "all") String range,
                                                    @RequestParam(defaultValue = "10") int top) {
        return Result.success(dashboardService.getHotProducts(range, top));
    }
}
