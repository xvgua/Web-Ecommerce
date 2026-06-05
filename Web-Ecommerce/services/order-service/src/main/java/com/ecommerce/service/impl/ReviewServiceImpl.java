package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.CreateFollowUpReviewRequest;
import com.ecommerce.dto.CreateReviewRequest;
import com.ecommerce.dto.ReviewRatingStats;
import com.ecommerce.entity.*;
import com.ecommerce.feign.ProductFeignClient;
import com.ecommerce.feign.UserFeignClient;
import com.ecommerce.mapper.ReviewCommentMapper;
import com.ecommerce.mapper.ReviewLikeMapper;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ReviewLikeMapper reviewLikeMapper;
    @Autowired
    private ReviewCommentMapper reviewCommentMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

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
        Result<Product> pr = productFeignClient.getProductById(productId);
        Product product = (pr.isSuccess() && pr.getData() != null) ? pr.getData() : null;
        BigDecimal avgRating = (product != null && product.getAvgRating() != null)
                ? product.getAvgRating() : BigDecimal.ZERO;
        long reviewCount = (product != null && product.getReviewCount() != null)
                ? product.getReviewCount() : 0;
        List<Map<String, Object>> rows = reviewMapper.getRatingDistribution(productId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);
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
        Result<Product> pr = productFeignClient.getProductById(request.getProductId());
        if (!pr.isSuccess() || pr.getData() == null) throw new BusinessException("商品不存在");
        Product product = pr.getData();

        Result<User> ur = userFeignClient.getUserById(userId);
        if (!ur.isSuccess() || ur.getData() == null) throw new BusinessException("用户不存在");
        User user = ur.getData();

        Long count = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(Review::getOrderId, request.getOrderId())
                        .eq(Review::getProductId, request.getProductId())
                        .eq(Review::getIsFollowup, 0));
        if (count > 0) throw new BusinessException("您已评价过该商品");

        BigDecimal overallRating = request.getRatingDesc().add(request.getRatingLogistics())
                .add(request.getRatingService())
                .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);

        Review review = new Review();
        review.setUserId(userId);
        review.setUsername(user.getUsername());
        review.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(overallRating);
        review.setRatingDesc(request.getRatingDesc());
        review.setRatingLogistics(request.getRatingLogistics());
        review.setRatingService(request.getRatingService());
        review.setContent(request.getContent());
        review.setIsFollowup(0);

        try {
            review.setImages(request.getImages() != null && !request.getImages().isEmpty()
                    ? objectMapper.writeValueAsString(request.getImages()) : "[]");
        } catch (JsonProcessingException e) { throw new BusinessException("图片数据处理失败"); }

        reviewMapper.insert(review);

        BigDecimal oldAvg = product.getAvgRating() != null ? product.getAvgRating() : BigDecimal.ZERO;
        int oldCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
        int newCount = oldCount + 1;
        BigDecimal newAvg = oldAvg.multiply(BigDecimal.valueOf(oldCount))
                .add(overallRating)
                .divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP);

        productFeignClient.updateRating(request.getProductId(), newAvg, newCount);
        return review;
    }

    @Override
    @Transactional
    public Review createFollowUpReview(Long userId, CreateFollowUpReviewRequest request) {
        Result<Product> pr = productFeignClient.getProductById(request.getProductId());
        if (!pr.isSuccess() || pr.getData() == null) throw new BusinessException("商品不存在");
        Result<User> ur = userFeignClient.getUserById(userId);
        if (!ur.isSuccess() || ur.getData() == null) throw new BusinessException("用户不存在");
        User user = ur.getData();

        Review review = new Review();
        review.setUserId(userId); review.setUsername(user.getUsername());
        review.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        review.setProductId(request.getProductId()); review.setOrderId(request.getOrderId());
        review.setRating(BigDecimal.ZERO); review.setContent(request.getContent()); review.setIsFollowup(1);
        try {
            review.setImages(request.getImages() != null && !request.getImages().isEmpty()
                    ? objectMapper.writeValueAsString(request.getImages()) : "[]");
        } catch (JsonProcessingException e) { throw new BusinessException("图片数据处理失败"); }
        reviewMapper.insert(review);
        return review;
    }

    @Override
    public PageResult<Review> getUserReviews(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        long total = reviewMapper.countByUserId(userId);
        List<Review> records = reviewMapper.selectByUserId(userId, offset, pageSize);
        if (!records.isEmpty()) {
            List<Long> reviewIds = records.stream().map(Review::getId).toList();
            Set<Long> likedIds = reviewLikeMapper.selectList(
                    new LambdaQueryWrapper<ReviewLike>().eq(ReviewLike::getUserId, userId)
                            .in(ReviewLike::getReviewId, reviewIds)).stream()
                    .map(ReviewLike::getReviewId).collect(Collectors.toSet());
            List<Review> allFollowUps = reviewMapper.selectList(
                    new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId)
                            .eq(Review::getIsFollowup, 1)
                            .in(Review::getOrderId, records.stream().map(Review::getOrderId).toList())
                            .orderByAsc(Review::getCreateTime));
            Map<String, List<Review>> followUpMap = allFollowUps.stream()
                    .collect(Collectors.groupingBy(r -> r.getOrderId() + "_" + r.getProductId()));
            for (Review review : records) {
                review.setIsLiked(likedIds.contains(review.getId()));
                String key = review.getOrderId() + "_" + review.getProductId();
                List<Review> fu = followUpMap.get(key);
                review.setHasFollowUp(fu != null && !fu.isEmpty());
                review.setFollowUpReviews(fu != null ? fu : Collections.emptyList());
            }
        }
        return PageResult.of(records, total, page, pageSize);
    }

    @Override
    public boolean likeReview(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) throw new BusinessException("评价不存在");
        if (reviewLikeMapper.selectCount(new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getUserId, userId).eq(ReviewLike::getReviewId, reviewId)) > 0) return false;
        ReviewLike like = new ReviewLike(); like.setUserId(userId); like.setReviewId(reviewId);
        reviewLikeMapper.insert(like);
        reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                .eq(Review::getId, reviewId).setSql("like_count = like_count + 1"));
        return true;
    }

    @Override
    public boolean unlikeReview(Long userId, Long reviewId) {
        int affected = reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getUserId, userId).eq(ReviewLike::getReviewId, reviewId));
        if (affected > 0) {
            reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                    .eq(Review::getId, reviewId).setSql("like_count = GREATEST(like_count - 1, 0)"));
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
        if (review == null) throw new BusinessException("评价不存在");
        Result<User> ur = userFeignClient.getUserById(userId);
        if (!ur.isSuccess() || ur.getData() == null) throw new BusinessException("用户不存在");
        User user = ur.getData();
        ReviewComment comment = new ReviewComment();
        comment.setReviewId(reviewId); comment.setUserId(userId);
        comment.setUsername(user.getUsername());
        comment.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        comment.setContent(content);
        reviewCommentMapper.insert(comment);
        reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                .eq(Review::getId, reviewId).setSql("comment_count = comment_count + 1"));
        return comment;
    }

    @Override
    public PageResult<Review> adminGetReviewPage(String keyword, String username, Integer rating,
                                                  String startDate, String endDate,
                                                  Boolean hasImage, Boolean hasFollowUp,
                                                  String sort, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String endDateAdjusted = endDate != null && !endDate.isEmpty() ? endDate + " 23:59:59" : null;
        long total = reviewMapper.countAdminList(keyword, username, rating, startDate, endDateAdjusted, hasImage, hasFollowUp);
        List<Review> records = reviewMapper.selectAdminList(keyword, username, rating, startDate, endDateAdjusted,
                hasImage, hasFollowUp, sort, offset, pageSize);
        if (!records.isEmpty()) {
            List<Long> orderIds = records.stream().map(Review::getOrderId).distinct().toList();
            List<Long> productIds = records.stream().map(Review::getProductId).distinct().toList();
            List<Review> followUps = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getIsFollowup, 1).in(Review::getOrderId, orderIds)
                    .in(Review::getProductId, productIds));
            Map<String, List<Review>> followUpMap = followUps.stream()
                    .collect(Collectors.groupingBy(r -> r.getOrderId() + "_" + r.getProductId()));
            for (Review r : records) {
                List<Review> fu = followUpMap.get(r.getOrderId() + "_" + r.getProductId());
                r.setHasFollowUp(fu != null && !fu.isEmpty());
            }
        }
        return PageResult.of(records, total, page, pageSize);
    }

    @Override
    public Review adminGetReviewDetail(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) throw new BusinessException("评价不存在");
        Result<Product> pr = productFeignClient.getProductById(review.getProductId());
        if (pr.isSuccess() && pr.getData() != null) {
            review.setProductName(pr.getData().getName());
            review.setProductImage(pr.getData().getMainImage());
            review.setProductPrice(pr.getData().getPrice());
        }
        review.setComments(reviewCommentMapper.selectByReviewId(id));
        List<Review> followUps = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getIsFollowup, 1).eq(Review::getOrderId, review.getOrderId())
                .eq(Review::getProductId, review.getProductId()));
        review.setHasFollowUp(!followUps.isEmpty());
        review.setFollowUpReviews(followUps);
        return review;
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) throw new BusinessException("评价不存在");
        reviewMapper.delete(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, review.getOrderId())
                .eq(Review::getProductId, review.getProductId()).eq(Review::getIsFollowup, 1));
        reviewCommentMapper.delete(new LambdaQueryWrapper<ReviewComment>().eq(ReviewComment::getReviewId, id));
        reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>().eq(ReviewLike::getReviewId, id));
        reviewMapper.deleteById(id);
        recalculateProductRating(review.getProductId());
    }

    @Override
    @Transactional
    public void adminBatchDeleteReviews(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException("请选择要删除的评价");
        List<Review> reviews = reviewMapper.selectBatchIds(ids);
        Set<Long> productIds = reviews.stream().map(Review::getProductId).collect(Collectors.toSet());
        for (Review review : reviews) {
            reviewMapper.delete(new LambdaQueryWrapper<Review>()
                    .eq(Review::getOrderId, review.getOrderId())
                    .eq(Review::getProductId, review.getProductId()).eq(Review::getIsFollowup, 1));
            reviewCommentMapper.delete(new LambdaQueryWrapper<ReviewComment>().eq(ReviewComment::getReviewId, review.getId()));
            reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>().eq(ReviewLike::getReviewId, review.getId()));
        }
        reviewMapper.deleteBatchIds(ids);
        for (Long pid : productIds) recalculateProductRating(pid);
    }

    private void recalculateProductRating(Long productId) {
        Result<Product> pr = productFeignClient.getProductById(productId);
        if (!pr.isSuccess() || pr.getData() == null) return;
        long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId).eq(Review::getIsFollowup, 0));
        if (count == 0) {
            productFeignClient.updateRating(productId, BigDecimal.ZERO, 0);
        } else {
            List<Review> productReviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getProductId, productId).eq(Review::getIsFollowup, 0));
            BigDecimal totalRating = productReviews.stream().map(Review::getRating)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newAvg = totalRating.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
            productFeignClient.updateRating(productId, newAvg, (int) count);
        }
    }
}
