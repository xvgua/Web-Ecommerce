package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.SeckillActivityForm;
import com.ecommerce.dto.SeckillOrderRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.SeckillActivity;
import com.ecommerce.entity.SeckillProduct;

import java.util.List;

public interface SeckillService {

    // Client
    List<SeckillActivity> getActiveActivities();
    List<SeckillActivity> getAllActivities();
    SeckillActivity getActivityDetail(Long activityId);
    Order createSeckillOrder(Long userId, SeckillOrderRequest req);
    SeckillProduct getSeckillProductDetail(Long seckillProductId);
    List<Long> getUserPurchasedProductIds(Long userId);

    // Admin
    PageResult<SeckillActivity> adminGetPage(int page, int pageSize, String keyword, Integer status);
    SeckillActivity adminGetById(Long id);
    SeckillActivity adminCreate(SeckillActivityForm form);
    void adminUpdate(Long id, SeckillActivityForm form);
    void adminDelete(Long id);

    // Scheduled
    void releaseTimeoutOrders();
}
