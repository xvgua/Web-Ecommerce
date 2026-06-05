package com.ecommerce.service;

public interface SearchLogService {
    void record(String keyword, Long userId);
}
