package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Announcement;
import com.ecommerce.mapper.AnnouncementMapper;
import com.ecommerce.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public PageResult<Announcement> getAnnouncementPage(PageQuery query) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getSortOrder)
               .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Announcement> result = announcementMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public List<Announcement> getLatest(int limit) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 1)
               .orderByDesc(Announcement::getSortOrder)
               .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> page = new Page<>(1, limit);
        return announcementMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public PageResult<Announcement> getPublishedPage(PageQuery query) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 1)
               .orderByDesc(Announcement::getSortOrder)
               .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Announcement> result = announcementMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public Announcement getById(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return a;
    }

    @Override
    public Announcement create(Announcement announcement) {
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    public void update(Long id, Announcement announcement) {
        announcement.setId(id);
        announcementMapper.updateById(announcement);
    }

    @Override
    public void delete(Long id) {
        if (announcementMapper.selectById(id) == null) {
            throw new BusinessException(404, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }
}
