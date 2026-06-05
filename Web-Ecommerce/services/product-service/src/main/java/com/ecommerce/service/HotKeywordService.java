package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.HotKeywordForm;
import com.ecommerce.entity.HotKeyword;

import java.util.List;

public interface HotKeywordService {
    List<HotKeyword> getHotKeywords(int limit);
    void computeAndRefresh(int days, int limit);
    PageResult<HotKeyword> adminGetPage(int page, int pageSize, String keyword);
    HotKeyword adminCreate(HotKeywordForm form);
    void adminUpdate(Long id, HotKeywordForm form);
    void adminDelete(Long id);
    void adminTogglePin(Long id);
    void adminToggleStatus(Long id);
}
