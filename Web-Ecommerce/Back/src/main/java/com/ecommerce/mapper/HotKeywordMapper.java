package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.HotKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HotKeywordMapper extends BaseMapper<HotKeyword> {

    List<HotKeyword> selectTopKeywords(@Param("days") int days,
                                        @Param("limit") int limit);

    int upsertComputed(@Param("keyword") String keyword,
                       @Param("searchCount") int searchCount,
                       @Param("days") int days,
                       @Param("limit") int limit);

    int disableOldComputed(@Param("days") int days,
                           @Param("limit") int limit);
}
