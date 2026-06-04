package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.HotKeywordForm;
import com.ecommerce.entity.HotKeyword;
import com.ecommerce.service.HotKeywordService;
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
@DisplayName("AdminHotKeywordController")
class AdminHotKeywordControllerTest {

    @Mock
    private HotKeywordService hotKeywordService;

    @InjectMocks
    private AdminHotKeywordController controller;

    private HotKeyword kw;

    @BeforeEach
    void setUp() {
        kw = new HotKeyword();
        kw.setId(1L);
        kw.setKeyword("手机");
        kw.setSearchCount(100);
        kw.setIsManual(0);
        kw.setIsPinned(0);
        kw.setStatus(1);
    }

    @Nested
    @DisplayName("List")
    class ListTests {

        @Test
        @DisplayName("should return paginated hot keywords")
        void shouldListHotKeywords() {
            PageResult<HotKeyword> page = PageResult.of(List.of(kw), 1L, 1, 10);
            when(hotKeywordService.adminGetPage(1, 10, null)).thenReturn(page);

            Result<PageResult<HotKeyword>> result = controller.list(1, 10, null);

            assertEquals(200, result.getCode());
            assertEquals(1, result.getData().getTotal());
            assertEquals(1, result.getData().getRecords().size());
            assertEquals("手机", result.getData().getRecords().get(0).getKeyword());
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        @DisplayName("should create a hot keyword")
        void shouldCreateHotKeyword() {
            HotKeywordForm form = new HotKeywordForm();
            form.setKeyword("新关键词");
            form.setIsPinned(1);

            HotKeyword created = new HotKeyword();
            created.setId(2L);
            created.setKeyword("新关键词");
            created.setIsManual(1);
            created.setIsPinned(1);

            when(hotKeywordService.adminCreate(form)).thenReturn(created);

            Result<HotKeyword> result = controller.create(form);

            assertEquals(200, result.getCode());
            assertEquals("新关键词", result.getData().getKeyword());
            assertEquals(1, result.getData().getIsManual());
            assertEquals(1, result.getData().getIsPinned());
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        @DisplayName("should update a hot keyword")
        void shouldUpdateHotKeyword() {
            HotKeywordForm form = new HotKeywordForm();
            form.setKeyword("更新关键词");
            form.setIsPinned(0);

            doNothing().when(hotKeywordService).adminUpdate(1L, form);

            Result<Void> result = controller.update(1L, form);

            assertEquals(200, result.getCode());
            verify(hotKeywordService).adminUpdate(1L, form);
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        @DisplayName("should delete a hot keyword")
        void shouldDeleteHotKeyword() {
            doNothing().when(hotKeywordService).adminDelete(1L);

            Result<Void> result = controller.delete(1L);

            assertEquals(200, result.getCode());
            verify(hotKeywordService).adminDelete(1L);
        }
    }

    @Nested
    @DisplayName("Toggle Pin")
    class TogglePinTests {

        @Test
        @DisplayName("should toggle pin status")
        void shouldTogglePin() {
            doNothing().when(hotKeywordService).adminTogglePin(1L);

            Result<Void> result = controller.togglePin(1L);

            assertEquals(200, result.getCode());
            verify(hotKeywordService).adminTogglePin(1L);
        }
    }

    @Nested
    @DisplayName("Toggle Status")
    class ToggleStatusTests {

        @Test
        @DisplayName("should toggle enable/disable status")
        void shouldToggleStatus() {
            doNothing().when(hotKeywordService).adminToggleStatus(1L);

            Result<Void> result = controller.toggleStatus(1L);

            assertEquals(200, result.getCode());
            verify(hotKeywordService).adminToggleStatus(1L);
        }
    }
}
