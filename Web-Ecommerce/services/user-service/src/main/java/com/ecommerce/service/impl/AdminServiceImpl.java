package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Admin;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.AdminMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;
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
