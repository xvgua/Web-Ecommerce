package com.ecommerce.service.impl;

import com.ecommerce.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerificationCode(String to, String code, String subject) {
        if (mailSender == null) {
            log.warn("Mail service not configured. Verification code for {}: {}", to, code);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            String html = """
                    <div style="max-width:480px;margin:0 auto;padding:24px;font-family:Arial,sans-serif;background:#f9fafb;border-radius:8px;">
                      <h2 style="color:#333;margin-bottom:16px;">电商平台 — 邮箱验证</h2>
                      <p style="color:#666;margin-bottom:24px;">您正在进行安全验证，请输入以下验证码：</p>
                      <div style="background:#fff;padding:20px;text-align:center;border-radius:8px;margin-bottom:24px;border:1px dashed #ddd;">
                        <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#409eff;">%s</span>
                        <p style="color:#999;font-size:12px;margin-top:8px;">验证码 5 分钟内有效，请勿泄露</p>
                      </div>
                      <p style="color:#999;font-size:12px;">如非本人操作，请忽略此邮件。</p>
                    </div>
                    """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification code sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification code to {}: {}", to, e.getMessage());
        }
    }
}
