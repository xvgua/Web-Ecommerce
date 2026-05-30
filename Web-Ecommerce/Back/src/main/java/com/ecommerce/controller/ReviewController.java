package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.CreateFollowUpReviewRequest;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.entity.Review;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
