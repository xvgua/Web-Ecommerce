package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Announcement;

public interface AnnouncementService {
    PageResult<Announcement> getAnnouncementPage(PageQuery query);
    Announcement create(Announcement announcement);
    void update(Long id, Announcement announcement);
    void delete(Long id);
}
