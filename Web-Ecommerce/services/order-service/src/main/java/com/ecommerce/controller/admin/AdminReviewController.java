package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.Review;
import com.ecommerce.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public Result<PageResult<Review>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) Integer rating,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate,
                                           @RequestParam(required = false) Boolean hasImage,
                                           @RequestParam(required = false) Boolean hasFollowUp,
                                           @RequestParam(required = false) String sort) {
        return Result.success(reviewService.adminGetReviewPage(
                keyword, username, rating, startDate, endDate,
                hasImage, hasFollowUp, sort, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Review> detail(@PathVariable Long id) {
        return Result.success(reviewService.adminGetReviewDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.adminDeleteReview(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        reviewService.adminBatchDeleteReviews(body.get("ids"));
        return Result.success();
    }
}
