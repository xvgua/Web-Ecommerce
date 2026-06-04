package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.HotKeywordForm;
import com.ecommerce.entity.HotKeyword;
import com.ecommerce.mapper.HotKeywordMapper;
import com.ecommerce.service.HotKeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class HotKeywordServiceImpl implements HotKeywordService {

    @Autowired
    private HotKeywordMapper hotKeywordMapper;

    @Override
    public List<HotKeyword> getHotKeywords(int limit) {
        LambdaQueryWrapper<HotKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotKeyword::getStatus, 1)
                .orderByDesc(HotKeyword::getIsPinned)
                .orderByDesc(HotKeyword::getSearchCount)
                .last("LIMIT " + Math.min(limit, 50));
        List<HotKeyword> list = hotKeywordMapper.selectList(wrapper);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @Transactional
    public void computeAndRefresh(int days, int limit) {
        List<HotKeyword> topKeywords = hotKeywordMapper.selectTopKeywords(days, limit);
        hotKeywordMapper.disableOldComputed(days, limit);
        for (HotKeyword kw : topKeywords) {
            hotKeywordMapper.upsertComputed(kw.getKeyword(), kw.getSearchCount(), days, limit);
        }
    }

    @Override
    public PageResult<HotKeyword> adminGetPage(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<HotKeyword> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(HotKeyword::getKeyword, keyword);
        }
        wrapper.orderByDesc(HotKeyword::getIsPinned)
                .orderByDesc(HotKeyword::getSearchCount)
                .orderByAsc(HotKeyword::getId);

        Page<HotKeyword> result = hotKeywordMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public HotKeyword adminCreate(HotKeywordForm form) {
        HotKeyword kw = new HotKeyword();
        kw.setKeyword(form.getKeyword());
        kw.setIsManual(1);
        kw.setIsPinned(form.getIsPinned() != null ? form.getIsPinned() : 0);
        kw.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
        kw.setSearchCount(0);
        kw.setStatus(1);
        hotKeywordMapper.insert(kw);
        return kw;
    }

    @Override
    public void adminUpdate(Long id, HotKeywordForm form) {
        HotKeyword kw = hotKeywordMapper.selectById(id);
        if (kw == null) {
            throw new BusinessException(404, "关键词不存在");
        }
        kw.setKeyword(form.getKeyword());
        if (form.getIsPinned() != null) {
            kw.setIsPinned(form.getIsPinned());
        }
        if (form.getSortOrder() != null) {
            kw.setSortOrder(form.getSortOrder());
        }
        hotKeywordMapper.updateById(kw);
    }

    @Override
    public void adminDelete(Long id) {
        hotKeywordMapper.deleteById(id);
    }

    @Override
    public void adminTogglePin(Long id) {
        HotKeyword kw = hotKeywordMapper.selectById(id);
        if (kw != null) {
            kw.setIsPinned(kw.getIsPinned() == 1 ? 0 : 1);
            hotKeywordMapper.updateById(kw);
        }
    }

    @Override
    public void adminToggleStatus(Long id) {
        HotKeyword kw = hotKeywordMapper.selectById(id);
        if (kw != null) {
            kw.setStatus(kw.getStatus() == 1 ? 0 : 1);
            hotKeywordMapper.updateById(kw);
        }
    }
}
