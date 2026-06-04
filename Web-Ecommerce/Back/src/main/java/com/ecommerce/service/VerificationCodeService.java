package com.ecommerce.service;

public interface VerificationCodeService {
    void generateAndSend(String email, String type);
    void verify(String email, String code, String type);
}
