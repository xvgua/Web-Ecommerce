package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.HotProductDTO;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.dto.SalesTrendDTO;
import com.ecommerce.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Map<String, Object> login(String username, String password);
    Map<String, Object> getDashboardStats();
    List<SalesTrendDTO> getSalesTrend(String range);
    List<HotProductDTO> getHotProducts(String range, int top);
    PageResult<User> getUserPage(PageQuery query);
    void toggleUserStatus(Long userId, Integer status);
}
