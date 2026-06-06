package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.SeckillActivity;
import com.ecommerce.entity.SeckillProduct;
import com.ecommerce.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/activities/all")
    public Result<List<SeckillActivity>> getAllActivities() {
        return Result.success(seckillService.getAllActivities());
    }

    @GetMapping("/activities")
    public Result<List<SeckillActivity>> getActiveActivities() {
        return Result.success(seckillService.getActiveActivities());
    }

    @GetMapping("/activities/{id}")
    public Result<SeckillActivity> getActivityDetail(@PathVariable Long id) {
        return Result.success(seckillService.getActivityDetail(id));
    }

    @GetMapping("/product/{id}")
    public Result<SeckillProduct> getSeckillProductDetail(@PathVariable Long id) {
        return Result.success(seckillService.getSeckillProductDetail(id));
    }
}
