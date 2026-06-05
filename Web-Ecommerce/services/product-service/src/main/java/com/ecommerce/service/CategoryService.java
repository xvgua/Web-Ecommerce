package com.ecommerce.service;

import com.ecommerce.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getCategoryTree();
    List<Category> getAllCategories();
    Category getById(Long id);
    Category create(Category category);
    void update(Long id, Category category);
    void delete(Long id);
    void moveSortOrder(Long id, String direction);
}
