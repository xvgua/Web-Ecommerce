package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.ResetPasswordRequest;
import com.ecommerce.dto.SendCodeRequest;
import com.ecommerce.service.UserService;
import com.ecommerce.service.VerificationCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        return userService.register(req);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @PostMapping("/send-register-code")
    public Result<Void> sendRegisterCode(@Valid @RequestBody SendCodeRequest req) {
        verificationCodeService.generateAndSend(req.getEmail(), "register");
        return Result.success("验证码已发送", null);
    }

    @PostMapping("/send-reset-code")
    public Result<Void> sendResetCode(@Valid @RequestBody SendCodeRequest req) {
        verificationCodeService.generateAndSend(req.getEmail(), "reset");
        return Result.success("验证码已发送", null);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return userService.resetPassword(req);
    }
}
