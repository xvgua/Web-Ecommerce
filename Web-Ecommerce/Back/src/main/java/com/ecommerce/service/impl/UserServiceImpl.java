package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.Result;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserStatus;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.service.UserService;
import com.ecommerce.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private VerificationCodeService verificationCodeService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Result<Void> register(RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        // Verify captcha
        verificationCodeService.verify(req.getEmail(), req.getCaptcha());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
        wrapper.clear();
        wrapper.eq(User::getEmail, req.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setNickname(req.getUsername());
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);

        log.info("User registered: username={}", req.getUsername());
        return Result.success();
    }

    @Override
    public Result<Map<String, Object>> login(LoginRequest req) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("账号已被禁用");
        }
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        boolean remember = req.getRemember() != null && req.getRemember();
        String token = jwtUtils.generateToken(user.getId(), "USER", remember);
        user.setPassword(null);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        log.info("User logged in: username={}", req.getUsername());
        return Result.success(data);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserInfo(Long userId) {
        return getUserById(userId);
    }

    @Override
    public Result<Void> updateUserInfo(Long userId, User user) {
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getNickname() != null) dbUser.setNickname(user.getNickname());
        if (user.getAvatar() != null) dbUser.setAvatar(user.getAvatar());
        if (user.getPhone() != null) dbUser.setPhone(user.getPhone());
        if (user.getEmail() != null) dbUser.setEmail(user.getEmail());
        userMapper.updateById(dbUser);
        return Result.success();
    }
}
