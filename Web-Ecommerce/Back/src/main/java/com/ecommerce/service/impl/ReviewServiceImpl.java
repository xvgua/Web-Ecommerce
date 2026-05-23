package com.ecommerce.service.impl;

import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.dto.ReviewRatingStats;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    @Transactional
    public Review createReview(Long userId, CreateReviewRequest request) {
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        review.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                review.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("图片数据处理失败");
            }
        } else {
            review.setImages("[]");
        }

        reviewMapper.insert(review);

        BigDecimal oldAvg = product.getAvgRating() != null ? product.getAvgRating() : BigDecimal.ZERO;
        int oldCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
        int newCount = oldCount + 1;
        BigDecimal newAvg = oldAvg
                .multiply(BigDecimal.valueOf(oldCount))
                .add(BigDecimal.valueOf(request.getRating()))
                .divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP);

        product.setAvgRating(newAvg);
        product.setReviewCount(newCount);
        productMapper.updateById(product);

        return review;
    }
}
