package com.ecommerce.controller.admin;

import com.ecommerce.common.Result;
import com.ecommerce.dto.CategorySalesDTO;
import com.ecommerce.dto.HotProductDTO;
import com.ecommerce.dto.SalesTrendDTO;
import com.ecommerce.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(adminService.getDashboardStats());
    }

    @GetMapping("/sales-trend")
    public Result<List<SalesTrendDTO>> salesTrend(@RequestParam(defaultValue = "7d") String range) {
        return Result.success(adminService.getSalesTrend(range));
    }

    @GetMapping("/hot-products")
    public Result<List<HotProductDTO>> hotProducts(@RequestParam(defaultValue = "all") String range,
                                                    @RequestParam(defaultValue = "10") int top) {
        return Result.success(adminService.getHotProducts(range, top));
    }

    @GetMapping("/category-sales")
    public Result<List<CategorySalesDTO>> categorySales() {
        return Result.success(adminService.getCategorySales());
    }
}
