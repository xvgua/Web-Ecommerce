package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Category> getCategoryTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0) {
                c.setChildren(childrenMap.getOrDefault(c.getId(), new ArrayList<>()));
                roots.add(c);
            }
        }
        return roots;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
    }

    @Override
    public Category getById(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(404, "分类不存在");
        return c;
    }

    @Override
    public Category create(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public void update(Long id, Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)) > 0) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }
        if (productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)) > 0) {
            throw new BusinessException("该分类下有商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
