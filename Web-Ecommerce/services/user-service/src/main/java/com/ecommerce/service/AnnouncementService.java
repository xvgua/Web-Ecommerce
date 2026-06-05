package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Announcement;

import java.util.List;

public interface AnnouncementService {
    PageResult<Announcement> getAnnouncementPage(PageQuery query);
    List<Announcement> getLatest(int limit);
    PageResult<Announcement> getPublishedPage(PageQuery query);
    Announcement getById(Long id);
    Announcement create(Announcement announcement);
    void update(Long id, Announcement announcement);
    void delete(Long id);
}
