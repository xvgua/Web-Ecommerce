package com.ecommerce.service;

import com.ecommerce.entity.SearchLog;
import com.ecommerce.mapper.SearchLogMapper;
import com.ecommerce.service.impl.SearchLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchLogService")
class SearchLogServiceTest {

    @Mock
    private SearchLogMapper searchLogMapper;

    @InjectMocks
    private SearchLogServiceImpl searchLogService;

    @Test
    @DisplayName("should save keyword and userId")
    void shouldRecordSearch() {
        searchLogService.record("手机", 1L);

        ArgumentCaptor<SearchLog> captor = ArgumentCaptor.forClass(SearchLog.class);
        verify(searchLogMapper).insert(captor.capture());

        SearchLog saved = captor.getValue();
        assertEquals("手机", saved.getKeyword());
        assertEquals(1L, saved.getUserId());
    }

    @Test
    @DisplayName("should record search without userId for anonymous users")
    void shouldRecordSearchWithoutUser() {
        searchLogService.record("笔记本电脑", null);

        ArgumentCaptor<SearchLog> captor = ArgumentCaptor.forClass(SearchLog.class);
        verify(searchLogMapper).insert(captor.capture());

        SearchLog saved = captor.getValue();
        assertEquals("笔记本电脑", saved.getKeyword());
        assertNull(saved.getUserId());
    }

    @Test
    @DisplayName("should not throw when recording empty keyword")
    void shouldHandleEmptyKeyword() {
        assertDoesNotThrow(() -> searchLogService.record("", null));
    }
}
