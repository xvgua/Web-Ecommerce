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
            "r.rating, r.content, r.images, r.create_time " +
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
            "WHERE product_id = #{productId} GROUP BY rating")
    List<Map<String, Object>> getRatingDistribution(@Param("productId") Long productId);
}
