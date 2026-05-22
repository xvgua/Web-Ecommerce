package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Announcement;
import com.ecommerce.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public Result<PageResult<Announcement>> list(PageQuery query) {
        return Result.success(announcementService.getAnnouncementPage(query));
    }

    @PostMapping
    public Result<Announcement> create(@RequestBody Announcement announcement) {
        return Result.success(announcementService.create(announcement));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcementService.update(id, announcement);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }
}
