package com.ecommerce.controller.admin;

import com.ecommerce.common.Result;
import com.ecommerce.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        Map<String, Object> data = adminService.login(body.get("username"), body.get("password"));
        return Result.success(data);
    }
}
