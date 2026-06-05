package com.ecommerce.service;

import com.ecommerce.dto.HotProductDTO;
import com.ecommerce.dto.SalesTrendDTO;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardStats();
    List<SalesTrendDTO> getSalesTrend(String range);
    List<HotProductDTO> getHotProducts(String range, int top);
}
