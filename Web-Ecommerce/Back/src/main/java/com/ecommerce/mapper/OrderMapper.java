package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS orderCount, " +
            "COALESCE(SUM(pay_amount), 0) AS salesAmount " +
            "FROM `order` WHERE status NOT IN (0, 4, 5) AND create_time >= #{start} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectSalesTrend(LocalDateTime start);
}
