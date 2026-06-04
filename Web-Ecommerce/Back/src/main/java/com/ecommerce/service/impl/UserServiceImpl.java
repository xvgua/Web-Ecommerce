package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ChangePasswordRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.ResetPasswordRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        verificationCodeService.verify(req.getEmail(), req.getCaptcha(), "register");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername())
               .or()
               .eq(User::getEmail, req.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名或邮箱已被注册");
        }

        User user = new User();
        user.setAccountId(generateAccountId());
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
            throw new BusinessException(401, "用户不存在");
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
            throw new BusinessException(401, "用户不存在");
        }
        if (user.getNickname() != null) dbUser.setNickname(user.getNickname());
        if (user.getAvatar() != null) dbUser.setAvatar(user.getAvatar());
        if (user.getPhone() != null) dbUser.setPhone(user.getPhone());
        if (user.getEmail() != null) dbUser.setEmail(user.getEmail());
        if (user.getGender() != null) dbUser.setGender(user.getGender());
        if (user.getIntro() != null) dbUser.setIntro(user.getIntro());

        // Username update: once per month, must be unique
        if (user.getUsername() != null && !user.getUsername().equals(dbUser.getUsername())) {
            LocalDateTime lastUpdate = dbUser.getUsernameUpdateTime();
            if (lastUpdate != null && lastUpdate.plusMonths(1).isAfter(LocalDateTime.now())) {
                throw new BusinessException("用户名每月只能修改一次");
            }
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("用户名已存在");
            }
            dbUser.setUsername(user.getUsername());
            dbUser.setUsernameUpdateTime(LocalDateTime.now());
        }

        userMapper.updateById(dbUser);
        return Result.success();
    }

    @Override
    public Result<Void> resetPassword(ResetPasswordRequest req) {
        verificationCodeService.verify(req.getEmail(), req.getCode(), "reset");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, req.getEmail());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("操作失败，请稍后重试");
        }

        user.setPassword(encoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

        log.info("Password reset for email={}", req.getEmail());
        return Result.success();
    }

    @Override
    public Result<Void> changePassword(Long userId, ChangePasswordRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        if (!encoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        String encoded = encoder.encode(req.getNewPassword());
        int rows = userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getPassword, encoded));
        if (rows == 0) {
            throw new BusinessException("密码修改失败");
        }

        log.info("Password changed for userId={}", userId);
        return Result.success();
    }

    private synchronized long generateAccountId() {
        LocalDate today = LocalDate.now();
        int datePart = (today.getYear() % 100) * 10000
                     + today.getMonthValue() * 100
                     + today.getDayOfMonth();
        long prefix = datePart * 100L;

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(User::getAccountId, prefix);
        wrapper.lt(User::getAccountId, prefix + 100);
        wrapper.orderByDesc(User::getAccountId);
        wrapper.last("LIMIT 1");
        User last = userMapper.selectOne(wrapper);

        long seq = 1;
        if (last != null) {
            seq = (last.getAccountId() % 100) + 1;
        }
        if (seq > 99) {
            throw new BusinessException("今日注册名额已满，请明天再试");
        }
        return prefix + seq;
    }
}
