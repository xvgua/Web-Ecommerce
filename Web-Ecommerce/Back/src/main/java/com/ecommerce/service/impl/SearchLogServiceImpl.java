package com.ecommerce.service.impl;

import com.ecommerce.entity.SearchLog;
import com.ecommerce.mapper.SearchLogMapper;
import com.ecommerce.service.SearchLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchLogServiceImpl implements SearchLogService {

    @Autowired
    private SearchLogMapper searchLogMapper;

    @Override
    public void record(String keyword, Long userId) {
        SearchLog log = new SearchLog();
        log.setKeyword(keyword);
        log.setUserId(userId);
        searchLogMapper.insert(log);
    }
}
