package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Announcement;
import com.ecommerce.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public Result<List<Announcement>> list(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(announcementService.getLatest(limit));
    }

    @GetMapping("/page")
    public Result<PageResult<Announcement>> page(PageQuery query) {
        return Result.success(announcementService.getPublishedPage(query));
    }
}
