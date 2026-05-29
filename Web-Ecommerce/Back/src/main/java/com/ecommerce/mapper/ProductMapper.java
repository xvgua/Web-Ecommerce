package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    List<Product> searchByKeyword(@Param("escapedKeyword") String escapedKeyword,
                                  @Param("likeKeyword") String likeKeyword,
                                  @Param("fuzzyKeyword") String fuzzyKeyword,
                                  @Param("status") Integer status,
                                  @Param("categoryIds") List<Long> categoryIds,
                                  @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    long countByKeyword(@Param("escapedKeyword") String escapedKeyword,
                        @Param("likeKeyword") String likeKeyword,
                        @Param("fuzzyKeyword") String fuzzyKeyword,
                        @Param("status") Integer status,
                        @Param("categoryIds") List<Long> categoryIds);
}
