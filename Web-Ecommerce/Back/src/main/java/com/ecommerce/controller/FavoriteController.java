package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.AddFavoriteRequest;
import com.ecommerce.entity.Favorite;
import com.ecommerce.entity.Product;
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
        return Result.success(favoriteService.addFavorite(UserContext.getUserId(), req.getProductId()));
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.removeFavorite(UserContext.getUserId(), productId);
        return Result.success();
    }

    @GetMapping
    public Result<List<Product>> list() {
        return Result.success(favoriteService.getFavoriteList(UserContext.getUserId()));
    }

    @GetMapping("/{productId}")
    public Result<Map<String, Boolean>> check(@PathVariable Long productId) {
        boolean favorited = favoriteService.isFavorited(UserContext.getUserId(), productId);
        return Result.success(Map.of("favorited", favorited));
    }
}
