package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.HotKeyword;
import com.ecommerce.service.HotKeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hot-keywords")
public class HotKeywordController {

    @Autowired
    private HotKeywordService hotKeywordService;

    @GetMapping
    public Result<List<HotKeyword>> list(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(hotKeywordService.getHotKeywords(limit));
    }
}
