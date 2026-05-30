package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.MailService;
import com.ecommerce.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final String CODE_PREFIX = "register:code:";
    private static final String COOLDOWN_PREFIX = "register:cooldown:";
    private static final String ATTEMPTS_PREFIX = "register:attempts:";
    private static final int CODE_TTL_MINUTES = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserMapper userMapper;

    private final SecureRandom random = new SecureRandom();

    @Override
    public void generateAndSend(String email) {
        // Check if email is already registered
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        // Cooldown check
        String cooldownKey = COOLDOWN_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long remaining = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            throw new BusinessException("操作过于频繁，请 " + (remaining != null ? remaining : 60) + " 秒后再试");
        }

        // Generate 6-digit code
        String code = String.format("%06d", random.nextInt(1_000_000));

        // Store code with 5-minute TTL
        String codeKey = CODE_PREFIX + email;
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // Set cooldown (60s)
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        // Reset attempt counter
        redisTemplate.delete(ATTEMPTS_PREFIX + email);

        // Send email
        mailService.sendVerificationCode(email, code);

        log.info("Verification code generated for {}", email);
    }

    @Override
    public void verify(String email, String code) {
        String codeKey = CODE_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        // Check if code exists
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        // Check max attempts
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            throw new BusinessException("验证码尝试次数过多，请重新获取");
        }

        // Verify
        if (!storedCode.equals(code)) {
            attempts++;
            // Set attempts counter with same TTL as the code
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts),
                    CODE_TTL_MINUTES, TimeUnit.MINUTES);
            int remaining = MAX_ATTEMPTS - attempts;
            throw new BusinessException("验证码错误" + (remaining > 0 ? "，还剩 " + remaining + " 次机会" : ""));
        }

        // Success — delete code and attempts (one-time use)
        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(COOLDOWN_PREFIX + email);

        log.info("Verification code verified for {}", email);
    }
}
