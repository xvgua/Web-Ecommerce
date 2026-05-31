package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateFollowUpReviewRequest;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.ReviewComment;

import java.util.List;

public interface ReviewService {
    PageResult<Review> getProductReviews(Long productId, int page, int pageSize,
                                         Integer ratingMin, Integer ratingMax);

    Review createReview(Long userId, CreateReviewRequest request);

    Review createFollowUpReview(Long userId, CreateFollowUpReviewRequest request);

    PageResult<Review> getUserReviews(Long userId, int page, int pageSize);

    boolean likeReview(Long userId, Long reviewId);

    boolean unlikeReview(Long userId, Long reviewId);

    List<ReviewComment> getReviewComments(Long reviewId);

    ReviewComment addReviewComment(Long userId, Long reviewId, String content);
}
