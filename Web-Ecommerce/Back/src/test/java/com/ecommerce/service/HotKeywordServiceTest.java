package com.ecommerce.service;

import com.ecommerce.entity.HotKeyword;
import com.ecommerce.mapper.HotKeywordMapper;
import com.ecommerce.service.impl.HotKeywordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotKeywordService")
class HotKeywordServiceTest {

    @Mock
    private HotKeywordMapper hotKeywordMapper;

    @InjectMocks
    private HotKeywordServiceImpl hotKeywordService;

    @Nested
    @DisplayName("getHotKeywords")
    class GetHotKeywords {

        @Test
        @DisplayName("should return top keywords limited by count")
        void shouldGetHotKeywords() {
            HotKeyword kw1 = buildKeyword(1L, "手机", 100, 0, 0, 1);
            HotKeyword kw2 = buildKeyword(2L, "耳机", 80, 0, 0, 1);
            HotKeyword kw3 = buildKeyword(3L, "电脑", 60, 0, 0, 1);

            when(hotKeywordMapper.selectList(any()))
                    .thenReturn(List.of(kw1, kw2, kw3));

            List<HotKeyword> result = hotKeywordService.getHotKeywords(10);

            assertEquals(3, result.size());
            assertEquals("手机", result.get(0).getKeyword());
        }

        @Test
        @DisplayName("should return pinned keywords before unpinned ones")
        void shouldPinKeywordsFirst() {
            HotKeyword kw1 = buildKeyword(1L, "手机", 100, 0, 0, 1);
            HotKeyword kw2 = buildKeyword(2L, "耳机", 80, 0, 1, 1);  // pinned
            HotKeyword kw3 = buildKeyword(3L, "电脑", 60, 0, 0, 1);

            // Service should order: pinned first, then by search_count desc
            when(hotKeywordMapper.selectList(any()))
                    .thenReturn(List.of(kw2, kw1, kw3));

            List<HotKeyword> result = hotKeywordService.getHotKeywords(10);

            assertEquals(3, result.size());
            assertEquals("耳机", result.get(0).getKeyword()); // pinned first
        }

        @Test
        @DisplayName("should only return enabled keywords")
        void shouldReturnEnabledOnly() {
            HotKeyword kw1 = buildKeyword(1L, "手机", 100, 0, 0, 1);
            HotKeyword kw2 = buildKeyword(2L, "禁用词", 80, 0, 0, 0); // disabled

            when(hotKeywordMapper.selectList(any()))
                    .thenReturn(List.of(kw1));

            List<HotKeyword> result = hotKeywordService.getHotKeywords(10);

            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(k -> k.getStatus() == 0));
        }

        @Test
        @DisplayName("should return empty list when no data")
        void shouldReturnEmptyWhenNoData() {
            List<HotKeyword> result = hotKeywordService.getHotKeywords(10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("computeAndRefresh")
    class ComputeAndRefresh {

        @Test
        @DisplayName("should compute top keywords from search_log and upsert")
        void shouldComputeHotKeywords() {
            HotKeyword computed = buildKeyword(null, "手机", 100, 0, 0, 1);

            when(hotKeywordMapper.selectTopKeywords(anyInt(), anyInt()))
                    .thenReturn(List.of(computed));
            when(hotKeywordMapper.disableOldComputed(anyInt(), anyInt())).thenReturn(1);
            when(hotKeywordMapper.upsertComputed(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(1);

            assertDoesNotThrow(() -> hotKeywordService.computeAndRefresh(7, 10));

            verify(hotKeywordMapper).selectTopKeywords(7, 10);
            verify(hotKeywordMapper).disableOldComputed(7, 10);
        }
    }

    private HotKeyword buildKeyword(Long id, String keyword, int searchCount,
                                     int isManual, int isPinned, int status) {
        HotKeyword kw = new HotKeyword();
        kw.setId(id);
        kw.setKeyword(keyword);
        kw.setSearchCount(searchCount);
        kw.setIsManual(isManual);
        kw.setIsPinned(isPinned);
        kw.setStatus(status);
        return kw;
    }
}
