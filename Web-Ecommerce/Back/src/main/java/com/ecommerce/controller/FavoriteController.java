package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.AddFavoriteRequest;
import com.ecommerce.dto.BatchFavoriteRequest;
import com.ecommerce.entity.Favorite;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping
    public Result<Favorite> add(@Valid @RequestBody AddFavoriteRequest req) {
        return Result.success(favoriteService.addFavorite(UserContext.getUserId(), req.getProductId(), req.getSkuId()));
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.removeFavorite(UserContext.getUserId(), productId);
        return Result.success();
    }

    @GetMapping
    public Result<List<Favorite>> list() {
        return Result.success(favoriteService.getFavoriteList(UserContext.getUserId()));
    }

    @PostMapping("/batch")
    public Result<Void> batchAdd(@Valid @RequestBody BatchFavoriteRequest req) {
        favoriteService.batchAddFavorites(UserContext.getUserId(), req.getItems());
        return Result.success();
    }

    @PutMapping("/{productId}/sku")
    public Result<Void> updateSku(@PathVariable Long productId, @RequestBody Map<String, Long> body) {
        favoriteService.updateFavoriteSku(UserContext.getUserId(), productId, body.get("skuId"));
        return Result.success();
    }

    @GetMapping("/{productId}")
    public Result<Map<String, Boolean>> check(@PathVariable Long productId) {
        boolean favorited = favoriteService.isFavorited(UserContext.getUserId(), productId);
        return Result.success(Map.of("favorited", favorited));
    }
}
