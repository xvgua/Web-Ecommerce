package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.entity.Review;

public interface ReviewService {
    PageResult<Review> getProductReviews(Long productId, int page, int pageSize,
                                         Integer ratingMin, Integer ratingMax);
}
