package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.CreateFollowUpReviewRequest;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.ReviewComment;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/reviews")
    public Result<Review> createReview(@Valid @RequestBody CreateReviewRequest request) {
        Long userId = UserContext.getUserId();
        Review review = reviewService.createReview(userId, request);
        return Result.success(review);
    }

    @PostMapping("/reviews/{productId}/followup")
    public Result<Review> createFollowUpReview(@PathVariable Long productId,
                                               @Valid @RequestBody CreateFollowUpReviewRequest request) {
        Long userId = UserContext.getUserId();
        request.setProductId(productId);
        Review review = reviewService.createFollowUpReview(userId, request);
        return Result.success(review);
    }

    @GetMapping("/reviews/mine")
    public Result<PageResult<Review>> getMyReviews(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getUserId();
        return Result.success(reviewService.getUserReviews(userId, page, pageSize));
    }

    @PostMapping("/reviews/{id}/like")
    public Result<Map<String, Object>> likeReview(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean liked = reviewService.likeReview(userId, id);
        return Result.success(Map.of("liked", liked));
    }

    @DeleteMapping("/reviews/{id}/like")
    public Result<Map<String, Object>> unlikeReview(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean unliked = reviewService.unlikeReview(userId, id);
        return Result.success(Map.of("unliked", unliked));
    }

    @GetMapping("/reviews/{id}/comments")
    public Result<List<ReviewComment>> getReviewComments(@PathVariable Long id) {
        return Result.success(reviewService.getReviewComments(id));
    }

    @PostMapping("/reviews/{id}/comments")
    public Result<ReviewComment> addReviewComment(@PathVariable Long id,
                                                   @Valid @RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return Result.error(400, "评论内容不能为空");
        }
        ReviewComment comment = reviewService.addReviewComment(userId, id, content.trim());
        return Result.success(comment);
    }
}
