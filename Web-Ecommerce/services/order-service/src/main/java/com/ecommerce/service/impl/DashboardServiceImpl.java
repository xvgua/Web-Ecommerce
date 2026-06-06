package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.Result;
import com.ecommerce.dto.HotProductDTO;
import com.ecommerce.dto.SalesTrendDTO;
import com.ecommerce.entity.*;
import com.ecommerce.feign.ProductFeignClient;
import com.ecommerce.feign.UserFeignClient;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public Map<String, Object> getDashboardStats() {
        Long totalUsers = 0L;
        try {
            Result<List<com.ecommerce.entity.User>> ur = userFeignClient.getAllUsers();
            if (ur.isSuccess() && ur.getData() != null) totalUsers = (long) ur.getData().size();
        } catch (Exception e) {
            log.warn("Failed to fetch user count from user-service", e);
        }

        Long totalProducts = 0L;
        try {
            Result<Long> pr = productFeignClient.getProductCount();
            if (pr.isSuccess() && pr.getData() != null) totalProducts = pr.getData();
        } catch (Exception e) {
            log.warn("Failed to fetch product count from product-service", e);
        }

        Long totalOrders = orderMapper.selectCount(null);

        BigDecimal totalSales = BigDecimal.ZERO;
        List<Order> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().ne(Order::getStatus, OrderStatus.CANCELLED));
        for (Order o : allOrders) {
            if (o.getPayAmount() != null && o.getPayAmount().compareTo(BigDecimal.ZERO) > 0)
                totalSales = totalSales.add(o.getPayAmount());
            else if (o.getTotalAmount() != null)
                totalSales = totalSales.add(o.getTotalAmount());
        }

        LocalDate today = LocalDate.now();
        Long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, today.atStartOfDay()));
        BigDecimal todaySales = BigDecimal.ZERO;
        List<Order> todayList = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, today.atStartOfDay()));
        for (Order o : todayList) {
            if (o.getTotalAmount() != null && o.getStatus() != OrderStatus.CANCELLED)
                todaySales = todaySales.add(o.getTotalAmount());
        }

        Long pendingOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.PENDING_PAY));
        Long shippingOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_SHIP).or().eq(Order::getStatus, OrderStatus.SHIPPED));
        Long completedOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.COMPLETED));
        Long cancelledOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.CANCELLED));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalProducts", totalProducts);
        stats.put("totalOrders", totalOrders);
        stats.put("totalSales", totalSales);
        stats.put("todayOrders", todayOrders);
        stats.put("todaySales", todaySales);
        stats.put("pendingOrders", pendingOrders);
        stats.put("shippingOrders", shippingOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("cancelledOrders", cancelledOrders);
        return stats;
    }

    @Override
    public List<SalesTrendDTO> getSalesTrend(String range) {
        LocalDateTime start = computeStart(range);
        List<Map<String, Object>> rows = orderMapper.selectSalesTrend(start);
        LocalDate cursor = start.toLocalDate();
        LocalDate today = LocalDate.now();
        Map<String, SalesTrendDTO> map = new LinkedHashMap<>();
        while (!cursor.isAfter(today)) {
            String key = cursor.toString();
            map.put(key, new SalesTrendDTO(key, 0L, BigDecimal.ZERO));
            cursor = cursor.plusDays(1);
        }
        for (Map<String, Object> row : rows) {
            String date = row.get("date").toString();
            Long orderCount = ((Number) row.get("orderCount")).longValue();
            BigDecimal salesAmount = (BigDecimal) row.get("salesAmount");
            map.put(date, new SalesTrendDTO(date, orderCount, salesAmount));
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public List<HotProductDTO> getHotProducts(String range, int top) {
        if ("all".equals(range)) {
            return getHotProductsFromProductSales(top);
        }

        List<Map<String, Object>> rows = orderItemMapper.selectHotProductsByTime(
                computeStart(range), top);

        if (rows.isEmpty()) {
            log.info("No order data for range={}, falling back to product sales", range);
            return getHotProductsFromProductSales(top);
        }

        return mapHotProductRows(rows);
    }

    private List<HotProductDTO> getHotProductsFromProductSales(int top) {
        try {
            Result<List<Product>> res = productFeignClient.getTopProductsBySales(top);
            if (res.isSuccess() && res.getData() != null) {
                return res.getData().stream()
                        .map(p -> new HotProductDTO(
                                p.getId(), p.getName(), p.getMainImage(),
                                p.getSales() != null ? p.getSales().longValue() : 0L,
                                p.getPrice() != null ? p.getPrice().multiply(
                                        BigDecimal.valueOf(p.getSales() != null ? p.getSales() : 0))
                                        : BigDecimal.ZERO))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch hot products from product-service", e);
        }
        return Collections.emptyList();
    }

    private List<HotProductDTO> mapHotProductRows(List<Map<String, Object>> rows) {
        Map<Long, HotProductDTO> dtoMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long productId = ((Number) row.get("productId")).longValue();
            Long sales = ((Number) row.get("sales")).longValue();
            BigDecimal salesAmount = (BigDecimal) row.get("salesAmount");

            try {
                Result<Product> pr = productFeignClient.getProductById(productId);
                if (pr.isSuccess() && pr.getData() != null) {
                    Product p = pr.getData();
                    dtoMap.put(productId, new HotProductDTO(p.getId(), p.getName(), p.getMainImage(), sales, salesAmount));
                }
            } catch (Exception e) {
                dtoMap.put(productId, new HotProductDTO(productId, "商品#" + productId, "", sales, salesAmount));
            }
        }
        return new ArrayList<>(dtoMap.values());
    }

    @Override
    public List<com.ecommerce.dto.CategorySalesDTO> getCategorySales() {
        try {
            Result<List<com.ecommerce.dto.CategorySalesDTO>> res = productFeignClient.getCategorySales();
            if (res.isSuccess() && res.getData() != null) return res.getData();
        } catch (Exception e) {
            log.warn("Failed to fetch category sales from product-service", e);
        }
        return Collections.emptyList();
    }

    private LocalDateTime computeStart(String range) {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case "7d" -> today.minusDays(6).atStartOfDay();
            case "30d" -> today.minusDays(29).atStartOfDay();
            case "month" -> today.withDayOfMonth(1).atStartOfDay();
            case "week" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            default -> today.minusDays(6).atStartOfDay();
        };
    }
}
