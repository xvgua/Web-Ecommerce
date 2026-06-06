package com.ecommerce.controller.admin;

import com.ecommerce.common.Result;
import com.ecommerce.dto.ChangePasswordRequest;
import com.ecommerce.entity.Admin;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public Result<Admin> getProfile() {
        Long adminId = UserContext.getUserId();
        return Result.success(adminService.getCurrentAdmin(adminId));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        Long adminId = UserContext.getUserId();
        adminService.changePassword(adminId, body.getOldPassword(), body.getNewPassword());
        return Result.success();
    }
}
