package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.HotKeywordForm;
import com.ecommerce.entity.HotKeyword;
import com.ecommerce.service.HotKeywordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hot-keywords")
public class AdminHotKeywordController {

    @Autowired
    private HotKeywordService hotKeywordService;

    @GetMapping
    public Result<PageResult<HotKeyword>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(hotKeywordService.adminGetPage(page, pageSize, keyword));
    }

    @PostMapping
    public Result<HotKeyword> create(@Valid @RequestBody HotKeywordForm form) {
        return Result.success(hotKeywordService.adminCreate(form));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HotKeywordForm form) {
        hotKeywordService.adminUpdate(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        hotKeywordService.adminDelete(id);
        return Result.success();
    }

    @PutMapping("/{id}/pin")
    public Result<Void> togglePin(@PathVariable Long id) {
        hotKeywordService.adminTogglePin(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        hotKeywordService.adminToggleStatus(id);
        return Result.success();
    }
}
