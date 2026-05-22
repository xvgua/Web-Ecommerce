package com.ecommerce.service;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.UpdateCartRequest;
import com.ecommerce.entity.Cart;

import java.util.List;

public interface CartService {
    List<Cart> getCartList(Long userId);
    void addToCart(Long userId, AddToCartRequest req);
    void updateCartItem(Long userId, Long cartId, UpdateCartRequest req);
    void removeCartItem(Long userId, Long cartId);
    void clearCart(Long userId);
}
