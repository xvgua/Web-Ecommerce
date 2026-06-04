package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Banner;
import java.util.List;

public interface BannerService {
    PageResult<Banner> getBannerPage(PageQuery query);
    List<Banner> getAllBanners();
    Banner getById(Long id);
    Banner create(Banner banner);
    void update(Long id, Banner banner);
    void delete(Long id);
    void toggleStatus(Long id, Integer status);
}
