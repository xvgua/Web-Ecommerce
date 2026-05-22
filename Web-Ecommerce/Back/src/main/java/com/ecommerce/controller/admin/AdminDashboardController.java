package com.ecommerce.controller.admin;

import com.ecommerce.common.Result;
import com.ecommerce.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
