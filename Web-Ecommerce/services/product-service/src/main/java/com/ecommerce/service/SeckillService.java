package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.SeckillActivityForm;
import com.ecommerce.entity.SeckillActivity;
import com.ecommerce.entity.SeckillProduct;

import java.util.List;

public interface SeckillService {

    List<SeckillActivity> getActiveActivities();
    SeckillActivity getActivityDetail(Long activityId);
    SeckillProduct getSeckillProductDetail(Long seckillProductId);

    PageResult<SeckillActivity> adminGetPage(int page, int pageSize, String keyword, Integer status);
    SeckillActivity adminGetById(Long id);
    SeckillActivity adminCreate(SeckillActivityForm form);
    void adminUpdate(Long id, SeckillActivityForm form);
    void adminDelete(Long id);
}
