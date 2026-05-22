package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.User;

import java.util.Map;

public interface AdminService {
    Map<String, Object> login(String username, String password);
    Map<String, Object> getDashboardStats();
    PageResult<User> getUserPage(PageQuery query);
    void toggleUserStatus(Long userId, Integer status);
}
