package com.ecommerce.service.impl;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ReviewRatingStats;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public PageResult<Review> getProductReviews(Long productId, int page, int pageSize,
                                               Integer ratingMin, Integer ratingMax) {
        int offset = (page - 1) * pageSize;

        long total = reviewMapper.countByProductId(productId, ratingMin, ratingMax);
        List<Review> records = reviewMapper.selectByProductId(productId, ratingMin, ratingMax, offset, pageSize);

        ReviewRatingStats stats = buildRatingStats(productId);
        return PageResult.of(records, total, page, pageSize, stats);
    }

    private ReviewRatingStats buildRatingStats(Long productId) {
        Product product = productMapper.selectById(productId);
        BigDecimal avgRating = product != null && product.getAvgRating() != null
                ? product.getAvgRating() : BigDecimal.ZERO;
        long reviewCount = product != null && product.getReviewCount() != null
                ? product.getReviewCount() : 0;

        List<Map<String, Object>> rows = reviewMapper.getRatingDistribution(productId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (Map<String, Object> row : rows) {
            Integer r = ((Number) row.get("rating")).intValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            distribution.put(r, cnt);
        }

        return new ReviewRatingStats(avgRating, reviewCount, distribution);
    }
}
