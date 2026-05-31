package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.ReviewComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewCommentMapper extends BaseMapper<ReviewComment> {

    @Select("SELECT * FROM review_comment WHERE review_id = #{reviewId} ORDER BY create_time ASC")
    List<ReviewComment> selectByReviewId(@Param("reviewId") Long reviewId);

    @Select("SELECT COUNT(*) FROM review_comment WHERE review_id = #{reviewId}")
    long countByReviewId(@Param("reviewId") Long reviewId);
}
