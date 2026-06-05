package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Banner;
import com.ecommerce.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping
    public Result<List<Banner>> list() {
        List<Banner> banners = bannerService.getAllBanners().stream()
                .filter(b -> b.getStatus() != null && b.getStatus() == 1)
                .limit(6)
                .collect(Collectors.toList());
        return Result.success(banners);
    }
}
