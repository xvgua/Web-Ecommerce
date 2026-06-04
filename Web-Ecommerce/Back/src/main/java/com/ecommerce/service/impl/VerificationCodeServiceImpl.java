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

    private String codeKey(String type, String email) {
        return "code:" + type + ":" + email;
    }

    private String cooldownKey(String type, String email) {
        return "cooldown:" + type + ":" + email;
    }

    private String attemptsKey(String type, String email) {
        return "attempts:" + type + ":" + email;
    }

    @Override
    public void generateAndSend(String email, String type) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);

        if ("register".equals(type)) {
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(409, "该邮箱已被使用");
            }
        } else if ("reset".equals(type)) {
            if (userMapper.selectCount(wrapper) == 0) {
                throw new BusinessException("验证码发送失败，请稍后重试");
            }
        }

        String ck = cooldownKey(type, email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ck))) {
            Long remaining = redisTemplate.getExpire(ck, TimeUnit.SECONDS);
            throw new BusinessException("操作过于频繁，请 " + (remaining != null ? remaining : 60) + " 秒后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        redisTemplate.opsForValue().set(codeKey(type, email), code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(ck, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
        redisTemplate.delete(attemptsKey(type, email));

        String subject = "register".equals(type) ? "邮箱验证码 - 电商平台注册" : "邮箱验证码 - 电商平台密码重置";
        mailService.sendVerificationCode(email, code, subject);

        log.info("Verification code ({}) generated for {}", type, email);
    }

    @Override
    public void verify(String email, String code, String type) {
        String ck = codeKey(type, email);
        String ak = attemptsKey(type, email);

        String storedCode = redisTemplate.opsForValue().get(ck);
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        String attemptsStr = redisTemplate.opsForValue().get(ak);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(ck);
            redisTemplate.delete(ak);
            throw new BusinessException("验证码尝试次数过多，请重新获取");
        }

        if (!storedCode.equals(code)) {
            attempts++;
            redisTemplate.opsForValue().set(ak, String.valueOf(attempts),
                    CODE_TTL_MINUTES, TimeUnit.MINUTES);
            int remaining = MAX_ATTEMPTS - attempts;
            throw new BusinessException("验证码错误" + (remaining > 0 ? "，还剩 " + remaining + " 次机会" : ""));
        }

        redisTemplate.delete(ck);
        redisTemplate.delete(ak);
        redisTemplate.delete(cooldownKey(type, email));

        log.info("Verification code ({}) verified for {}", type, email);
    }
}
