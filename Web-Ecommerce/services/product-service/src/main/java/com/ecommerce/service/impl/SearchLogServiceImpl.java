package com.ecommerce.service.impl;

import com.ecommerce.entity.SearchLog;
import com.ecommerce.mapper.SearchLogMapper;
import com.ecommerce.service.SearchLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SearchLogServiceImpl implements SearchLogService {

    @Autowired
    private SearchLogMapper searchLogMapper;

    @Override
    public void record(String keyword, Long userId) {
        try {
            SearchLog log = new SearchLog();
            log.setKeyword(keyword);
            log.setUserId(userId);
            searchLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("Failed to persist search log for '{}': {}", keyword, e.getMessage());
        }
    }
}
