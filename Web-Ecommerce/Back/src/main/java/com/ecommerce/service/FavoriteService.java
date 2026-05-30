package com.ecommerce.service;

import com.ecommerce.dto.BatchFavoriteItem;
import com.ecommerce.entity.Favorite;

import java.util.List;

public interface FavoriteService {
    Favorite addFavorite(Long userId, Long productId, Long skuId);
    void removeFavorite(Long userId, Long productId);
    void batchAddFavorites(Long userId, List<BatchFavoriteItem> items);
    List<Favorite> getFavoriteList(Long userId);
    boolean isFavorited(Long userId, Long productId);
    void updateFavoriteSku(Long userId, Long productId, Long skuId);
}
