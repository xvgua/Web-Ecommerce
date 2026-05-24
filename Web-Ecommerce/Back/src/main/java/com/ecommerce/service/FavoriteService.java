package com.ecommerce.service;

import com.ecommerce.entity.Favorite;
import com.ecommerce.entity.Product;

import java.util.List;

public interface FavoriteService {
    Favorite addFavorite(Long userId, Long productId);
    void removeFavorite(Long userId, Long productId);
    List<Product> getFavoriteList(Long userId);
    boolean isFavorited(Long userId, Long productId);
}
