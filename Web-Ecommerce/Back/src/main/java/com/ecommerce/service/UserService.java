package com.ecommerce.service;

import com.ecommerce.common.Result;
import com.ecommerce.dto.ChangePasswordRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.ResetPasswordRequest;
import com.ecommerce.entity.User;

import java.util.Map;

public interface UserService {
    Result<Void> register(RegisterRequest req);
    Result<Map<String, Object>> login(LoginRequest req);
    User getUserById(Long id);
    User getUserInfo(Long userId);
    Result<Void> updateUserInfo(Long userId, User user);
    Result<Void> resetPassword(ResetPasswordRequest req);
    Result<Void> changePassword(Long userId, ChangePasswordRequest req);
}
