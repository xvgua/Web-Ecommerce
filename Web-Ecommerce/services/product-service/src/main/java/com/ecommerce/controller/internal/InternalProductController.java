package com.ecommerce.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.Result;
import com.ecommerce.dto.CategorySalesDTO;
import com.ecommerce.dto.DeductStockRequest;
import com.ecommerce.dto.RestoreStockRequest;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductSku;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ProductSkuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/internal")
public class InternalProductController {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper skuMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping("/products/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        return Result.success(product);
    }

    @GetMapping("/skus/{id}")
    public Result<ProductSku> getSkuById(@PathVariable Long id) {
        ProductSku sku = skuMapper.selectById(id);
        if (sku == null) {
            return Result.error(404, "SKU不存在");
        }
        return Result.success(sku);
    }

    @PostMapping("/products/deduct-stock")
    public Result<Void> deductStock(@RequestBody DeductStockRequest req) {
        if (req.getSkuId() != null && req.getSkuId() > 0) {
            int affected = skuMapper.update(null,
                    new LambdaUpdateWrapper<ProductSku>()
                            .eq(ProductSku::getId, req.getSkuId())
                            .ge(ProductSku::getStock, req.getQuantity())
                            .setSql("stock = stock - " + req.getQuantity())
                            .setSql("sales = sales + " + req.getQuantity()));
            if (affected == 0) {
                return Result.error(400, "SKU库存不足");
            }
            productMapper.update(null,
                    new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, req.getProductId())
                            .setSql("sales = sales + " + req.getQuantity()));
        } else {
            int affected = productMapper.update(null,
                    new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, req.getProductId())
                            .ge(Product::getStock, req.getQuantity())
                            .setSql("stock = stock - " + req.getQuantity())
                            .setSql("sales = sales + " + req.getQuantity()));
            if (affected == 0) {
                return Result.error(400, "商品库存不足");
            }
        }
        return Result.success(null);
    }

    @PostMapping("/products/restore-stock")
    public Result<Void> restoreStock(@RequestBody RestoreStockRequest req) {
        productMapper.update(null,
                new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, req.getProductId())
                        .setSql("stock = stock + " + req.getQuantity()));
        if (req.getSkuId() != null && req.getSkuId() > 0) {
            skuMapper.update(null,
                    new LambdaUpdateWrapper<ProductSku>()
                            .eq(ProductSku::getId, req.getSkuId())
                            .setSql("stock = stock + " + req.getQuantity()));
        }
        return Result.success(null);
    }

    @GetMapping("/products/batch")
    public Result<List<Product>> batchGetProducts(@RequestParam("ids") List<Long> ids) {
        List<Product> products = productMapper.selectBatchIds(ids);
        return Result.success(products);
    }

    @PutMapping("/products/{id}/rating")
    public Result<Void> updateRating(@PathVariable Long id,
                                     @RequestParam("avgRating") java.math.BigDecimal avgRating,
                                     @RequestParam("reviewCount") Integer reviewCount) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        product.setAvgRating(avgRating);
        product.setReviewCount(reviewCount);
        productMapper.updateById(product);
        return Result.success(null);
    }

    @GetMapping("/products/top-sales")
    public Result<List<Product>> getTopProductsBySales(@RequestParam(defaultValue = "10") int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Product::getSales).last("LIMIT " + limit);
        List<Product> products = productMapper.selectList(wrapper);
        return Result.success(products);
    }

    @GetMapping("/products/count")
    public Result<Long> getProductCount() {
        return Result.success(productMapper.selectCount(null));
    }

    @GetMapping("/products/category-sales")
    public Result<List<CategorySalesDTO>> getCategorySales() {
        List<CategorySalesDTO> result = new ArrayList<>();
        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> nameMap = new HashMap<>();
        for (Category c : categories) {
            nameMap.put(c.getId(), c.getName());
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Product::getCategoryId, Product::getSales)
                .isNotNull(Product::getSales)
                .gt(Product::getSales, 0);
        List<Product> products = productMapper.selectList(wrapper);

        Map<Long, Long> salesMap = new LinkedHashMap<>();
        for (Product p : products) {
            Long catId = p.getCategoryId();
            salesMap.merge(catId, (long) (p.getSales() != null ? p.getSales() : 0), Long::sum);
        }

        for (Map.Entry<Long, Long> entry : salesMap.entrySet()) {
            String name = nameMap.getOrDefault(entry.getKey(), "未知分类");
            result.add(new CategorySalesDTO(name, entry.getValue()));
        }
        result.sort((a, b) -> b.getSales().compareTo(a.getSales()));
        return Result.success(result);
    }
}
