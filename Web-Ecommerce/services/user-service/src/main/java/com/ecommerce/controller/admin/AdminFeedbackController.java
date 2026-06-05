package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.FeedbackQuery;
import com.ecommerce.dto.FeedbackReplyRequest;
import com.ecommerce.entity.Feedback;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/feedbacks")
public class AdminFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public Result<PageResult<Feedback>> list(FeedbackQuery query) {
        return Result.success(feedbackService.adminGetPage(query));
    }

    @GetMapping("/{id}")
    public Result<Feedback> detail(@PathVariable Long id) {
        return Result.success(feedbackService.adminGetDetail(id));
    }

    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody FeedbackReplyRequest req) {
        feedbackService.reply(UserContext.getUserId(), id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return Result.success();
    }
}
