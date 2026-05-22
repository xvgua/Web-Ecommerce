package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.UpdateCartRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public Result<List<Cart>> list() {
        return Result.success(cartService.getCartList(UserContext.getUserId()));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddToCartRequest req) {
        cartService.addToCart(UserContext.getUserId(), req);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateCartRequest req) {
        cartService.updateCartItem(UserContext.getUserId(), id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.removeCartItem(UserContext.getUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clearCart(UserContext.getUserId());
        return Result.success();
    }
}
