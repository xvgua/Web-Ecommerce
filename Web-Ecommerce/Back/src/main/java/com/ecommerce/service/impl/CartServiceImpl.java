package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.UpdateCartRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductSku;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import com.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;

    @Override
    public List<Cart> getCartList(Long userId) {
        List<Cart> items = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        for (Cart item : items) {
            fillCartItem(item);
        }
        return items;
    }

    @Override
    public void addToCart(Long userId, AddToCartRequest req) {
        Product product = productMapper.selectById(req.getProductId());
        if (product == null) throw new BusinessException(404, "商品不存在");

        // Check if already in cart (upsert)
        Long skuId = req.getSkuId() != null ? req.getSkuId() : 0L;
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, req.getProductId())
                .eq(Cart::getSkuId, skuId);
        Cart existing = cartMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + req.getQuantity());
            cartMapper.updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(req.getProductId());
            cart.setSkuId(skuId);
            cart.setQuantity(req.getQuantity());
            cart.setChecked(1);
            cartMapper.insert(cart);
        }
    }

    @Override
    public void updateCartItem(Long userId, Long cartId, UpdateCartRequest req) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(404, "购物车商品不存在");
        }
        if (req.getQuantity() != null) {
            if (req.getQuantity() <= 0) {
                cartMapper.deleteById(cartId);
                return;
            }
            cart.setQuantity(req.getQuantity());
        }
        if (req.getChecked() != null) {
            cart.setChecked(req.getChecked() ? 1 : 0);
        }
        cartMapper.updateById(cart);
    }

    @Override
    public void removeCartItem(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(404, "购物车商品不存在");
        }
        cartMapper.deleteById(cartId);
    }

    @Override
    public void clearCart(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    private void fillCartItem(Cart item) {
        Product product = productMapper.selectById(item.getProductId());
        if (product != null) {
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(product.getPrice());
            item.setStock(product.getStock());
        }
        if (item.getSkuId() != null && item.getSkuId() > 0) {
            ProductSku sku = skuMapper.selectById(item.getSkuId());
            if (sku != null) {
                item.setSpecDesc(sku.getSpecName() + ":" + sku.getSpecValue());
                if (sku.getPrice() != null) item.setPrice(sku.getPrice());
                if (sku.getStock() != null) item.setStock(sku.getStock());
            }
        }
    }
}
