package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT oi.product_id AS productId, SUM(oi.quantity) AS sales, " +
            "COALESCE(SUM(oi.price * oi.quantity), 0) AS salesAmount " +
            "FROM order_item oi JOIN `order` o ON oi.order_id = o.id " +
            "WHERE o.status NOT IN (0, 4, 5) AND o.create_time >= #{start} " +
            "GROUP BY oi.product_id ORDER BY sales DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHotProductsByTime(@Param("start") LocalDateTime start, @Param("limit") int limit);
}
