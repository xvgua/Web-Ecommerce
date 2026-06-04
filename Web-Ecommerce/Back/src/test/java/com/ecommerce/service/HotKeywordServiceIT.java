package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.HotKeywordForm;
import com.ecommerce.entity.HotKeyword;
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
@DisplayName("HotKeywordService 集成测试 (H2)")
class HotKeywordServiceIT {

    @Autowired
    private HotKeywordService hotKeywordService;

    // ── getHotKeywords ──

    @Test
    @DisplayName("getHotKeywords: 应返回启用状态的热门词，置顶优先")
    void shouldReturnEnabledKeywordsPinnedFirst() {
        HotKeywordForm f1 = new HotKeywordForm();
        f1.setKeyword("热门词A");
        f1.setIsPinned(1);
        f1.setSortOrder(1);
        hotKeywordService.adminCreate(f1);

        HotKeywordForm f2 = new HotKeywordForm();
        f2.setKeyword("热门词B");
        f2.setIsPinned(0);
        f2.setSortOrder(0);
        hotKeywordService.adminCreate(f2);

        List<HotKeyword> keywords = hotKeywordService.getHotKeywords(10);

        assertTrue(keywords.size() >= 2);
        assertEquals(1, keywords.get(0).getIsPinned());
    }

    @Test
    @DisplayName("getHotKeywords: 应遵守 limit 限制")
    void shouldRespectLimit() {
        for (int i = 0; i < 5; i++) {
            HotKeywordForm f = new HotKeywordForm();
            f.setKeyword("测试词" + i);
            hotKeywordService.adminCreate(f);
        }

        List<HotKeyword> keywords = hotKeywordService.getHotKeywords(3);

        assertTrue(keywords.size() <= 3);
    }

    // ── admin CRUD ──

    @Test
    @DisplayName("adminCreate: 应创建手动关键词并返回带ID的实体")
    void shouldCreateManualKeyword() {
        HotKeywordForm form = new HotKeywordForm();
        form.setKeyword("测试关键词");
        form.setIsPinned(1);
        form.setSortOrder(10);

        HotKeyword created = hotKeywordService.adminCreate(form);

        assertNotNull(created.getId());
        assertEquals("测试关键词", created.getKeyword());
        assertEquals(1, created.getIsManual());
        assertEquals(1, created.getIsPinned());
        assertEquals(1, created.getStatus());
    }

    @Test
    @DisplayName("adminUpdate: 应更新关键词信息")
    void shouldUpdateKeyword() {
        HotKeywordForm form = new HotKeywordForm();
        form.setKeyword("原关键词");
        HotKeyword created = hotKeywordService.adminCreate(form);

        HotKeywordForm update = new HotKeywordForm();
        update.setKeyword("新关键词");
        update.setIsPinned(1);
        hotKeywordService.adminUpdate(created.getId(), update);

        PageResult<HotKeyword> result = hotKeywordService.adminGetPage(1, 10, "新关键词");
        assertEquals(1, result.getTotal());
        assertEquals("新关键词", result.getRecords().get(0).getKeyword());
    }

    @Test
    @DisplayName("adminDelete: 应删除关键词")
    void shouldDeleteKeyword() {
        HotKeywordForm form = new HotKeywordForm();
        form.setKeyword("待删除");
        HotKeyword created = hotKeywordService.adminCreate(form);

        hotKeywordService.adminDelete(created.getId());

        PageResult<HotKeyword> result = hotKeywordService.adminGetPage(1, 10, "待删除");
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("adminTogglePin: 应切换置顶状态")
    void shouldTogglePin() {
        HotKeywordForm form = new HotKeywordForm();
        form.setKeyword("切换置顶");
        HotKeyword created = hotKeywordService.adminCreate(form);

        assertEquals(0, created.getIsPinned());

        hotKeywordService.adminTogglePin(created.getId());
        HotKeyword afterPin = hotKeywordService.adminGetPage(1, 10, "切换置顶").getRecords().get(0);
        assertEquals(1, afterPin.getIsPinned());

        hotKeywordService.adminTogglePin(created.getId());
        HotKeyword afterUnpin = hotKeywordService.adminGetPage(1, 10, "切换置顶").getRecords().get(0);
        assertEquals(0, afterUnpin.getIsPinned());
    }

    @Test
    @DisplayName("adminToggleStatus: 应切换启用/禁用状态")
    void shouldToggleStatus() {
        HotKeywordForm form = new HotKeywordForm();
        form.setKeyword("切换状态");
        HotKeyword created = hotKeywordService.adminCreate(form);

        assertEquals(1, created.getStatus());

        hotKeywordService.adminToggleStatus(created.getId());
        // disabled keyword should not appear in public getHotKeywords
        List<HotKeyword> publicList = hotKeywordService.getHotKeywords(100);
        assertTrue(publicList.stream().noneMatch(k -> k.getId().equals(created.getId())));
    }

    // ── admin pagination ──

    @Test
    @DisplayName("adminGetPage: 应支持关键词搜索和分页")
    void shouldPageAndSearch() {
        HotKeywordForm f1 = new HotKeywordForm();
        f1.setKeyword("苹果手机");
        hotKeywordService.adminCreate(f1);

        HotKeywordForm f2 = new HotKeywordForm();
        f2.setKeyword("华为手机");
        hotKeywordService.adminCreate(f2);

        HotKeywordForm f3 = new HotKeywordForm();
        f3.setKeyword("笔记本电脑");
        hotKeywordService.adminCreate(f3);

        PageResult<HotKeyword> result = hotKeywordService.adminGetPage(1, 10, "手机");

        assertEquals(2, result.getTotal());
        result.getRecords().forEach(k ->
                assertTrue(k.getKeyword().contains("手机")));
    }
}
