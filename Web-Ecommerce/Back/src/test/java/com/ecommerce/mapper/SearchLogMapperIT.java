package com.ecommerce.mapper;

import com.ecommerce.entity.SearchLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("SearchLogMapper 集成测试")
class SearchLogMapperIT {

    @Autowired
    private SearchLogMapper searchLogMapper;

    @Test
    @DisplayName("insert + selectById 应正确持久化搜索日志")
    void shouldInsertAndFindById() {
        SearchLog log = new SearchLog();
        log.setKeyword("手机");
        log.setUserId(1L);

        int rows = searchLogMapper.insert(log);
        assertEquals(1, rows);
        assertNotNull(log.getId());

        SearchLog found = searchLogMapper.selectById(log.getId());
        assertNotNull(found);
        assertEquals("手机", found.getKeyword());
        assertEquals(1L, found.getUserId());
        assertNotNull(found.getCreateTime());
    }

    @Test
    @DisplayName("selectRecentByKeyword 应按时间倒序返回同关键词日志")
    void shouldSelectRecentByKeyword() throws InterruptedException {
        SearchLog log1 = new SearchLog();
        log1.setKeyword("电脑");
        log1.setUserId(1L);
        searchLogMapper.insert(log1);

        Thread.sleep(10);

        SearchLog log2 = new SearchLog();
        log2.setKeyword("电脑");
        log2.setUserId(2L);
        searchLogMapper.insert(log2);

        List<SearchLog> results = searchLogMapper.selectRecentByKeyword("电脑");

        assertTrue(results.size() >= 2);
        assertEquals("电脑", results.get(0).getKeyword());
        assertFalse(results.get(0).getCreateTime().isBefore(results.get(1).getCreateTime()),
                "最新的记录应排在前面");
    }

    @Test
    @DisplayName("selectRecentByKeyword 无匹配时应返回空列表")
    void shouldReturnEmptyForUnknownKeyword() {
        List<SearchLog> results = searchLogMapper.selectRecentByKeyword("不存在的关键词");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("匿名用户搜索（userId=null）应正常持久化")
    void shouldSaveAnonymousSearch() {
        SearchLog log = new SearchLog();
        log.setKeyword("耳机");
        log.setUserId(null);

        searchLogMapper.insert(log);

        SearchLog found = searchLogMapper.selectById(log.getId());
        assertNotNull(found);
        assertEquals("耳机", found.getKeyword());
        assertNull(found.getUserId());
    }
}
