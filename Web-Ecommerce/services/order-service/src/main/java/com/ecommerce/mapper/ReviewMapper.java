package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("<script>" +
            "SELECT r.id, r.user_id, r.username, r.avatar, r.product_id, r.order_id, " +
            "r.rating, r.rating_desc, r.rating_logistics, r.rating_service, " +
            "r.content, r.images, r.is_followup, r.like_count, r.comment_count, r.create_time " +
            "FROM review r WHERE r.product_id = #{productId}" +
            "<if test='ratingMin != null'> AND r.rating <![CDATA[ >= ]]> #{ratingMin}</if>" +
            "<if test='ratingMax != null'> AND r.rating <![CDATA[ <= ]]> #{ratingMax}</if>" +
            " ORDER BY r.create_time DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Review> selectByProductId(@Param("productId") Long productId,
                                   @Param("ratingMin") Integer ratingMin,
                                   @Param("ratingMax") Integer ratingMax,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM review WHERE product_id = #{productId}" +
            "<if test='ratingMin != null'> AND rating <![CDATA[ >= ]]> #{ratingMin}</if>" +
            "<if test='ratingMax != null'> AND rating <![CDATA[ <= ]]> #{ratingMax}</if>" +
            "</script>")
    long countByProductId(@Param("productId") Long productId,
                          @Param("ratingMin") Integer ratingMin,
                          @Param("ratingMax") Integer ratingMax);

    @Select("SELECT rating, COUNT(*) AS cnt FROM review " +
            "WHERE product_id = #{productId} AND is_followup = 0 GROUP BY rating")
    List<Map<String, Object>> getRatingDistribution(@Param("productId") Long productId);

    @Select("SELECT r.*, p.name AS product_name, p.main_image AS product_image, p.price AS product_price " +
            "FROM review r LEFT JOIN product p ON r.product_id = p.id " +
            "WHERE r.user_id = #{userId} AND r.is_followup = 0 " +
            "ORDER BY r.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<Review> selectByUserId(@Param("userId") Long userId,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM review WHERE user_id = #{userId} AND is_followup = 0")
    long countByUserId(@Param("userId") Long userId);

    List<Review> selectAdminList(@Param("keyword") String keyword,
                                  @Param("username") String username,
                                  @Param("rating") Integer rating,
                                  @Param("startDate") String startDate,
                                  @Param("endDate") String endDate,
                                  @Param("hasImage") Boolean hasImage,
                                  @Param("hasFollowUp") Boolean hasFollowUp,
                                  @Param("sort") String sort,
                                  @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    long countAdminList(@Param("keyword") String keyword,
                         @Param("username") String username,
                         @Param("rating") Integer rating,
                         @Param("startDate") String startDate,
                         @Param("endDate") String endDate,
                         @Param("hasImage") Boolean hasImage,
                         @Param("hasFollowUp") Boolean hasFollowUp);
}
