package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.User;
import com.ecommerce.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public Result<PageResult<User>> list(PageQuery query) {
        return Result.success(adminService.getUserPage(query));
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.toggleUserStatus(id, body.get("status"));
        return Result.success();
    }
}
