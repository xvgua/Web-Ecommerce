package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.ResetPasswordRequest;
import com.ecommerce.dto.SendCodeRequest;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.service.UserService;
import com.ecommerce.service.VerificationCodeService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtils.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remaining > 0) {
                    stringRedisTemplate.opsForValue().set(
                            "bl:" + userId + ":" + token.substring(0, 20),
                            "1", Duration.ofMillis(remaining));
                }
            } catch (Exception ignored) {
                // token already expired, no need to blacklist
            }
        }
        return Result.success("已登出", null);
    }
}
