package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.HotKeyword;
import com.ecommerce.service.HotKeywordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotKeywordController (public)")
class HotKeywordControllerTest {

    @Mock
    private HotKeywordService hotKeywordService;

    @InjectMocks
    private HotKeywordController controller;

    private HotKeyword kw1, kw2;

    @BeforeEach
    void setUp() {
        kw1 = new HotKeyword();
        kw1.setId(1L);
        kw1.setKeyword("手机");
        kw1.setSearchCount(100);
        kw1.setIsPinned(1);

        kw2 = new HotKeyword();
        kw2.setId(2L);
        kw2.setKeyword("电脑");
        kw2.setSearchCount(80);
        kw2.setIsPinned(0);
    }

    @Test
    @DisplayName("应返回热门关键词列表")
    void shouldReturnHotKeywords() {
        when(hotKeywordService.getHotKeywords(10)).thenReturn(Arrays.asList(kw1, kw2));

        Result<List<HotKeyword>> result = controller.list(10);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("手机", result.getData().get(0).getKeyword());
        assertEquals("电脑", result.getData().get(1).getKeyword());
    }

    @Test
    @DisplayName("应支持自定义 limit 参数")
    void shouldPassLimitParameter() {
        when(hotKeywordService.getHotKeywords(5)).thenReturn(Collections.emptyList());

        Result<List<HotKeyword>> result = controller.list(5);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verify(hotKeywordService).getHotKeywords(5);
    }

    @Test
    @DisplayName("空列表时应正常返回")
    void shouldReturnEmptyList() {
        when(hotKeywordService.getHotKeywords(10)).thenReturn(Collections.emptyList());

        Result<List<HotKeyword>> result = controller.list(10);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }
}
