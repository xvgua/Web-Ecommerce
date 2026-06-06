package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Admin;
import com.ecommerce.entity.User;

import java.util.Map;

public interface AdminService {
    Map<String, Object> login(String username, String password);
    PageResult<User> getUserPage(PageQuery query);
    User getUserById(Long id);
    void toggleUserStatus(Long userId, Integer status);
    Admin getCurrentAdmin(Long adminId);
    void changePassword(Long adminId, String oldPassword, String newPassword);
}
