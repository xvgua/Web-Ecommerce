package com.ecommerce.service;

public interface VerificationCodeService {
    void generateAndSend(String email);
    void verify(String email, String code);
}
