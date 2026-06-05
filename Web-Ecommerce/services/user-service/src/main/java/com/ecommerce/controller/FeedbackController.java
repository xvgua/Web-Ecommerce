package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.FeedbackSubmitRequest;
import com.ecommerce.entity.Feedback;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody FeedbackSubmitRequest req) {
        feedbackService.submit(UserContext.getUserId(), req);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<PageResult<Feedback>> myList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(feedbackService.getMyPage(UserContext.getUserId(), page, pageSize));
    }

    @GetMapping("/my/{id}")
    public Result<Feedback> myDetail(@PathVariable Long id) {
        return Result.success(feedbackService.getMyDetail(UserContext.getUserId(), id));
    }
}
