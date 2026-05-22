package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductForm;
import com.ecommerce.dto.ProductQuery;
import com.ecommerce.entity.*;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import com.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResult<Product> getProductPage(ProductQuery query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        } else {
            wrapper.eq(Product::getStatus, ProductStatus.ON_SALE);
        }

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Product::getName, query.getKeyword());
        }
        if (query.getCategoryId() != null && query.getCategoryId() > 0) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }

        String sort = query.getSort();
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sales_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getSales);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Product> result = productMapper.selectPage(page, wrapper);

        // Fill category names
        for (Product p : result.getRecords()) {
            if (p.getCategoryId() != null) {
                Category c = categoryMapper.selectById(p.getCategoryId());
                if (c != null) p.setCategoryName(c.getName());
            }
        }

        return PageResult.of(result.getRecords(), result.getTotal(),
                query.getPage(), query.getPageSize());
    }

    @Override
    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (product.getCategoryId() != null) {
            Category c = categoryMapper.selectById(product.getCategoryId());
            if (c != null) product.setCategoryName(c.getName());
        }
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        product.setSkus(skus);
        return product;
    }

    @Override
    public List<Product> getHotProducts(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ON_SALE)
                .orderByDesc(Product::getSales)
                .last("LIMIT " + Math.min(limit, 50));
        return productMapper.selectList(wrapper);
    }

    @Override
    public List<Product> getNewProducts(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ON_SALE)
                .orderByDesc(Product::getCreateTime)
                .last("LIMIT " + Math.min(limit, 50));
        return productMapper.selectList(wrapper);
    }

    @Override
    public Product create(ProductForm form) {
        Product product = new Product();
        product.setName(form.getName());
        product.setCategoryId(form.getCategoryId());
        product.setPrice(form.getPrice());
        product.setStock(form.getStock() != null ? form.getStock() : 0);
        product.setDescription(form.getDescription());
        product.setMainImage(form.getMainImage());
        product.setImages(form.getImages());
        product.setStatus(form.getStatus() != null ? form.getStatus() : ProductStatus.ON_SALE);
        product.setSales(0);
        productMapper.insert(product);
        return product;
    }

    @Override
    public void update(Long id, ProductForm form) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(404, "商品不存在");
        if (form.getName() != null) product.setName(form.getName());
        if (form.getCategoryId() != null) product.setCategoryId(form.getCategoryId());
        if (form.getPrice() != null) product.setPrice(form.getPrice());
        if (form.getStock() != null) product.setStock(form.getStock());
        if (form.getDescription() != null) product.setDescription(form.getDescription());
        if (form.getMainImage() != null) product.setMainImage(form.getMainImage());
        if (form.getImages() != null) product.setImages(form.getImages());
        if (form.getStatus() != null) product.setStatus(form.getStatus());
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        if (productMapper.selectById(id) == null) {
            throw new BusinessException(404, "商品不存在");
        }
        productMapper.deleteById(id);
        skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
    }
}
