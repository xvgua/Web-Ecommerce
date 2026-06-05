package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.dto.BatchFavoriteItem;
import com.ecommerce.entity.Favorite;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductSku;
import com.ecommerce.mapper.FavoriteMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import com.ecommerce.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;

    @Override
    public Favorite addFavorite(Long userId, Long productId, Long skuId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        Long effectiveSkuId = skuId != null ? skuId : 0L;
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (existing != null) {
            existing.setSkuId(effectiveSkuId);
            favoriteMapper.updateById(existing);
            log.info("Favorite: userId={}, productId={}, skuId={}, action=update_sku", userId, productId, effectiveSkuId);
            return existing;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setSkuId(effectiveSkuId);
        try {
            favoriteMapper.insert(favorite);
            log.info("Favorite: userId={}, productId={}, skuId={}, action=add", userId, productId, effectiveSkuId);
        } catch (DuplicateKeyException e) {
            existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getProductId, productId));
            if (existing != null) {
                existing.setSkuId(effectiveSkuId);
                favoriteMapper.updateById(existing);
            }
            favorite = existing;
        }
        return favorite;
    }

    @Override
    public void removeFavorite(Long userId, Long productId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        Favorite favorite = favoriteMapper.selectOne(wrapper);
        if (favorite == null) {
            log.info("Favorite: userId={}, productId={}, action=cancel_not_found", userId, productId);
            return;
        }
        favoriteMapper.deleteById(favorite.getId());
        log.info("Favorite: userId={}, productId={}, action=remove", userId, productId);
    }

    @Override
    public void batchAddFavorites(Long userId, List<BatchFavoriteItem> items) {
        for (BatchFavoriteItem item : items) {
            try {
                Long skuId = item.getSkuId() != null ? item.getSkuId() : 0L;
                Product product = productMapper.selectById(item.getProductId());
                if (product == null) continue;

                Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, item.getProductId()));
                if (existing != null) {
                    existing.setSkuId(skuId);
                    favoriteMapper.updateById(existing);
                } else {
                    Favorite favorite = new Favorite();
                    favorite.setUserId(userId);
                    favorite.setProductId(item.getProductId());
                    favorite.setSkuId(skuId);
                    try {
                        favoriteMapper.insert(favorite);
                    } catch (DuplicateKeyException e) {
                        // already exists, update sku
                        Favorite dup = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                                .eq(Favorite::getUserId, userId)
                                .eq(Favorite::getProductId, item.getProductId()));
                        if (dup != null) {
                            dup.setSkuId(skuId);
                            favoriteMapper.updateById(dup);
                        }
                    }
                }
            } catch (Exception e) {
                // skip invalid items
            }
        }
        log.info("Favorite batch: userId={}, count={}", userId, items.size());
    }

    @Override
    public List<Favorite> getFavoriteList(Long userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreateTime);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);

        for (Favorite fav : favorites) {
            Product product = productMapper.selectById(fav.getProductId());
            if (product != null) {
                fav.setProductName(product.getName());
                fav.setProductImage(product.getMainImage());
                fav.setPrice(product.getPrice());
                fav.setStock(product.getStock());
            }
            if (fav.getSkuId() != null && fav.getSkuId() > 0) {
                ProductSku sku = skuMapper.selectById(fav.getSkuId());
                if (sku != null) {
                    fav.setSpecDesc(sku.getSpecName() + ":" + sku.getSpecValue());
                    if (sku.getPrice() != null) fav.setPrice(sku.getPrice());
                    if (sku.getStock() != null) fav.setStock(sku.getStock());
                }
            }
        }
        return favorites;
    }

    @Override
    public void updateFavoriteSku(Long userId, Long productId, Long skuId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        Favorite favorite = favoriteMapper.selectOne(wrapper);
        if (favorite == null) {
            throw new BusinessException(404, "收藏不存在");
        }
        favorite.setSkuId(skuId);
        favoriteMapper.updateById(favorite);
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
