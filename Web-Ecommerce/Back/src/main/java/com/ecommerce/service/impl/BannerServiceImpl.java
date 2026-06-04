package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.PageQuery;
import com.ecommerce.entity.Banner;
import com.ecommerce.mapper.BannerMapper;
import com.ecommerce.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public PageResult<Banner> getBannerPage(PageQuery query) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Banner::getTitle, query.getKeyword());
        }
        wrapper.orderByAsc(Banner::getSortOrder);
        Page<Banner> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Banner> result = bannerMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public List<Banner> getAllBanners() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortOrder));
    }

    @Override
    public Banner create(Banner banner) {
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public void update(Long id, Banner banner) {
        banner.setId(id);
        bannerMapper.updateById(banner);
    }

    @Override
    public Banner getById(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(404, "轮播图不存在");
        }
        return banner;
    }

    @Override
    public void delete(Long id) {
        if (bannerMapper.selectById(id) == null) {
            throw new BusinessException(404, "轮播图不存在");
        }
        bannerMapper.deleteById(id);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(404, "轮播图不存在");
        }
        banner.setStatus(status);
        bannerMapper.updateById(banner);
    }
}
