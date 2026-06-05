package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.Result;
import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.UpdateCartRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductSku;
import com.ecommerce.feign.ProductFeignClient;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductFeignClient productFeignClient;

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
        Result<Product> prodResult = productFeignClient.getProductById(req.getProductId());
        if (prodResult.isError() || prodResult.getData() == null) {
            throw new BusinessException("商品不存在");
        }

        Long skuId = req.getSkuId() != null ? req.getSkuId() : 0L;
        Cart existing = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, req.getProductId())
                .eq(Cart::getSkuId, skuId));
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
            throw new BusinessException("购物车商品不存在");
        }
        if (req.getQuantity() != null) {
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
        if (cart != null && cart.getUserId().equals(userId)) {
            cartMapper.deleteById(cartId);
        }
    }

    @Override
    public void clearCart(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    private void fillCartItem(Cart item) {
        Result<Product> prodResult = productFeignClient.getProductById(item.getProductId());
        if (prodResult.isSuccess() && prodResult.getData() != null) {
            Product product = prodResult.getData();
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(product.getPrice());
            item.setStock(product.getStock());
        }
        if (item.getSkuId() != null && item.getSkuId() > 0) {
            Result<ProductSku> skuResult = productFeignClient.getSkuById(item.getSkuId());
            if (skuResult.isSuccess() && skuResult.getData() != null) {
                ProductSku sku = skuResult.getData();
                item.setPrice(sku.getPrice());
                item.setStock(sku.getStock());
                item.setSpecDesc(sku.getSpecName() + ":" + sku.getSpecValue());
                if (sku.getImage() != null) {
                    item.setProductImage(sku.getImage());
                }
            }
        }
    }
}
