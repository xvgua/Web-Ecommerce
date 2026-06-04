package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.SeckillActivityForm;
import com.ecommerce.entity.SeckillActivity;
import com.ecommerce.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/seckill")
public class AdminSeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping
    public Result<PageResult<SeckillActivity>> list(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(seckillService.adminGetPage(page, pageSize, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<SeckillActivity> detail(@PathVariable Long id) {
        return Result.success(seckillService.adminGetById(id));
    }

    @PostMapping
    public Result<SeckillActivity> create(@Valid @RequestBody SeckillActivityForm form) {
        return Result.success(seckillService.adminCreate(form));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SeckillActivityForm form) {
        seckillService.adminUpdate(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seckillService.adminDelete(id);
        return Result.success();
    }
}
