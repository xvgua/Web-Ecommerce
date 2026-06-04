package com.ecommerce.service.impl;

import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.CreateFollowUpReviewRequest;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.dto.ReviewRatingStats;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.ReviewComment;
import com.ecommerce.entity.ReviewLike;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ReviewCommentMapper;
import com.ecommerce.mapper.ReviewLikeMapper;
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
    @Autowired
    private ReviewLikeMapper reviewLikeMapper;
    @Autowired
    private ReviewCommentMapper reviewCommentMapper;

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

        // 检查是否已发表过初始评价（同一订单同一商品只能评价一次）
        Long count = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(Review::getOrderId, request.getOrderId())
                        .eq(Review::getProductId, request.getProductId())
                        .eq(Review::getIsFollowup, 0));
        if (count > 0) {
            throw new BusinessException("您已评价过该商品");
        }

        BigDecimal ratingDesc = request.getRatingDesc();
        BigDecimal ratingLogistics = request.getRatingLogistics();
        BigDecimal ratingService = request.getRatingService();
        BigDecimal overallRating = ratingDesc.add(ratingLogistics).add(ratingService)
                .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);

        Review review = new Review();
        review.setUserId(userId);
        review.setUsername(user.getUsername());
        review.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(overallRating);
        review.setRatingDesc(ratingDesc);
        review.setRatingLogistics(ratingLogistics);
        review.setRatingService(ratingService);
        review.setContent(request.getContent());
        review.setIsFollowup(0);

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
                .add(overallRating)
                .divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP);

        product.setAvgRating(newAvg);
        product.setReviewCount(newCount);
        productMapper.updateById(product);

        return review;
    }

    @Override
    @Transactional
    public Review createFollowUpReview(Long userId, CreateFollowUpReviewRequest request) {
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
        review.setUsername(user.getUsername());
        review.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(BigDecimal.ZERO);
        review.setContent(request.getContent());
        review.setIsFollowup(1);

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
        return review;
    }

    @Override
    public PageResult<Review> getUserReviews(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        long total = reviewMapper.countByUserId(userId);
        List<Review> records = reviewMapper.selectByUserId(userId, offset, pageSize);

        if (!records.isEmpty()) {
            // Batch check which reviews the user has liked
            List<Long> reviewIds = records.stream().map(Review::getId).toList();
            List<ReviewLike> likes = reviewLikeMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewLike>()
                            .eq(ReviewLike::getUserId, userId)
                            .in(ReviewLike::getReviewId, reviewIds));
            java.util.Set<Long> likedIds = likes.stream().map(ReviewLike::getReviewId)
                    .collect(java.util.stream.Collectors.toSet());

            // Batch fetch follow-up reviews (same user, is_followup=1)
            List<Review> allFollowUps = reviewMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                            .eq(Review::getUserId, userId)
                            .eq(Review::getIsFollowup, 1)
                            .in(Review::getOrderId, records.stream().map(Review::getOrderId).toList())
                            .orderByAsc(Review::getCreateTime));
            java.util.Map<String, java.util.List<Review>> followUpMap = allFollowUps.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            r -> r.getOrderId() + "_" + r.getProductId()));

            for (Review review : records) {
                review.setIsLiked(likedIds.contains(review.getId()));
                String key = review.getOrderId() + "_" + review.getProductId();
                java.util.List<Review> followUps = followUpMap.get(key);
                review.setHasFollowUp(followUps != null && !followUps.isEmpty());
                review.setFollowUpReviews(followUps != null ? followUps : java.util.Collections.emptyList());
            }
        }

        return PageResult.of(records, total, page, pageSize);
    }

    @Override
    public boolean likeReview(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        Long count = reviewLikeMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewLike>()
                        .eq(ReviewLike::getUserId, userId)
                        .eq(ReviewLike::getReviewId, reviewId));
        if (count > 0) {
            return false;
        }
        ReviewLike like = new ReviewLike();
        like.setUserId(userId);
        like.setReviewId(reviewId);
        reviewLikeMapper.insert(like);
        reviewMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Review>()
                        .eq(Review::getId, reviewId)
                        .setSql("like_count = like_count + 1"));
        return true;
    }

    @Override
    public boolean unlikeReview(Long userId, Long reviewId) {
        int affected = reviewLikeMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewLike>()
                        .eq(ReviewLike::getUserId, userId)
                        .eq(ReviewLike::getReviewId, reviewId));
        if (affected > 0) {
            reviewMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Review>()
                            .eq(Review::getId, reviewId)
                            .setSql("like_count = GREATEST(like_count - 1, 0)"));
            return true;
        }
        return false;
    }

    @Override
    public List<ReviewComment> getReviewComments(Long reviewId) {
        return reviewCommentMapper.selectByReviewId(reviewId);
    }

    @Override
    @Transactional
    public ReviewComment addReviewComment(Long userId, Long reviewId, String content) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        ReviewComment comment = new ReviewComment();
        comment.setReviewId(reviewId);
        comment.setUserId(userId);
        comment.setUsername(user.getUsername());
        comment.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        comment.setContent(content);
        reviewCommentMapper.insert(comment);
        reviewMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Review>()
                        .eq(Review::getId, reviewId)
                        .setSql("comment_count = comment_count + 1"));
        return comment;
    }
}
