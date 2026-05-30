package com.ecommerce.service;

public interface MailService {
    void sendVerificationCode(String to, String code);
}
