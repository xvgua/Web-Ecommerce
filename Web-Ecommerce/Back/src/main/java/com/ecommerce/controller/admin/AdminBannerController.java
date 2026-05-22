package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Banner;
import com.ecommerce.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping
    public Result<PageResult<Banner>> list(PageQuery query) {
        return Result.success(bannerService.getBannerPage(query));
    }

    @PostMapping
    public Result<Banner> create(@RequestBody Banner banner) {
        return Result.success(bannerService.create(banner));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Banner banner) {
        bannerService.update(id, banner);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.success();
    }
}
