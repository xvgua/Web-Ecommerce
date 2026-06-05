package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.HotProductDTO;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.dto.SalesTrendDTO;
import com.ecommerce.entity.*;
import com.ecommerce.mapper.*;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> login(String username, String password) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(wrapper);
        if (admin == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!encoder.matches(password, admin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(admin.getId(), admin.getRole(), false);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        log.info("Admin logged in: username={}", username);
        return data;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long totalOrders = orderMapper.selectCount(null);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Order::getTotalAmount);
        var orders = orderMapper.selectList(null);
        BigDecimal totalSales = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getTotalAmount() != null) {
                totalSales = totalSales.add(o.getTotalAmount());
            }
        }

        LocalDate today = LocalDate.now();
        Long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreateTime, today.atStartOfDay()));
        BigDecimal todaySales = BigDecimal.ZERO;
        var todayOrderList = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreateTime, today.atStartOfDay()));
        for (Order o : todayOrderList) {
            if (o.getTotalAmount() != null && o.getStatus() != OrderStatus.CANCELLED) {
                todaySales = todaySales.add(o.getTotalAmount());
            }
        }

        Long pendingOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.PENDING_PAY));
        Long shippingOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.PENDING_SHIP)
                        .or().eq(Order::getStatus, OrderStatus.SHIPPED));
        Long completedOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.COMPLETED));
        Long cancelledOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.CANCELLED));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
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
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(Product::getSales).last("LIMIT " + top);
            return productMapper.selectList(wrapper).stream()
                    .map(p -> new HotProductDTO(p.getId(), p.getName(), p.getMainImage(),
                            p.getSales() != null ? p.getSales() : 0L,
                            p.getPrice() != null ? p.getPrice().multiply(BigDecimal.valueOf(
                                    p.getSales() != null ? p.getSales() : 0)) : BigDecimal.ZERO))
                    .collect(Collectors.toList());
        }

        LocalDateTime start = computeStart(range);
        List<Map<String, Object>> rows = orderItemMapper.selectHotProductsByTime(start, top);

        Map<Long, HotProductDTO> dtoMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long productId = ((Number) row.get("productId")).longValue();
            Long sales = ((Number) row.get("sales")).longValue();
            BigDecimal salesAmount = (BigDecimal) row.get("salesAmount");

            Product p = productMapper.selectById(productId);
            if (p != null) {
                dtoMap.put(productId, new HotProductDTO(
                        p.getId(), p.getName(), p.getMainImage(), sales, salesAmount));
            }
        }
        return new ArrayList<>(dtoMap.values());
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

    @Override
    public PageResult<User> getUserPage(PageQuery query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(User::getUsername, query.getKeyword())
                    .or().like(User::getPhone, query.getKeyword());
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = new Page<>(query.getPage(), query.getPageSize());
        Page<User> result = userMapper.selectPage(page, wrapper);

        for (User u : result.getRecords()) {
            u.setPassword(null);
        }

        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public void toggleUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("User status toggled: userId={}, status={}", userId, status);
    }
}
