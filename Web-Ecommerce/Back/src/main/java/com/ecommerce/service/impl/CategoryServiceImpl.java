package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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

        Map<Long, List<Category>> childrenMap = new LinkedHashMap<>();
        for (Category c : all) {
            if (c.getParentId() != null && c.getParentId() > 0) {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }

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
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getParentId)
                        .orderByAsc(Category::getSortOrder));
    }

    @Override
    public Category getById(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(404, "分类不存在");
        return c;
    }

    @Override
    public Category create(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }

        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        category.setParentId(parentId);

        if (parentId > 0) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("上级分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                throw new BusinessException("仅支持两级分类，无法在二级分类下继续添加");
            }
        }

        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .eq(Category::getName, category.getName()));
        if (count > 0) {
            throw new BusinessException("同级下已存在同名分类");
        }

        List<Category> siblings = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, parentId)
                        .orderByDesc(Category::getSortOrder)
                        .last("LIMIT 1"));
        int nextSort = siblings.isEmpty() ? 1 : siblings.get(0).getSortOrder() + 1;
        category.setSortOrder(nextSort);

        categoryMapper.insert(category);
        return category;
    }

    @Override
    public void update(Long id, Category category) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }

        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }

        // Only validate and update parent when explicitly provided
        if (category.getParentId() != null) {
            Long parentId = category.getParentId() == 0 ? 0L : category.getParentId();

            if (parentId.equals(id)) {
                throw new BusinessException("不能将自身设为上级分类");
            }

            if (parentId > 0) {
                Category parent = categoryMapper.selectById(parentId);
                if (parent == null) {
                    throw new BusinessException("上级分类不存在");
                }
                if (parent.getParentId() != null && parent.getParentId() > 0) {
                    throw new BusinessException("仅支持两级分类，无法移动到二级分类下");
                }
                boolean hasChildren = categoryMapper.selectCount(
                        new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)) > 0;
                if (hasChildren) {
                    throw new BusinessException("该分类下有子分类，无法移动为子分类");
                }
            }

            Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                    .eq(Category::getParentId, parentId)
                    .eq(Category::getName, category.getName())
                    .ne(Category::getId, id));
            if (count > 0) {
                throw new BusinessException("同级下已存在同名分类");
            }

            category.setParentId(parentId);
        } else {
            // parentId not provided: keep existing parent, only validate name uniqueness
            Category ref = existing;
            Long currentParent = ref.getParentId() == null ? 0L : ref.getParentId();
            Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                    .eq(Category::getParentId, currentParent)
                    .eq(Category::getName, category.getName())
                    .ne(Category::getId, id));
            if (count > 0) {
                throw new BusinessException("同级下已存在同名分类");
            }
        }

        category.setId(id);
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException(404, "分类不存在");
        }
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

    @Override
    @Transactional
    public void moveSortOrder(Long id, String direction) {
        if (!"up".equals(direction) && !"down".equals(direction)) {
            throw new BusinessException("排序方向参数无效");
        }

        Category current = categoryMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(404, "分类不存在");
        }

        Long parentId = current.getParentId() == null ? 0L : current.getParentId();
        List<Category> siblings;
        if (parentId == 0L) {
            siblings = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>()
                            .and(w -> w.eq(Category::getParentId, 0L).or().isNull(Category::getParentId))
                            .orderByAsc(Category::getSortOrder));
        } else {
            siblings = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getParentId, parentId)
                            .orderByAsc(Category::getSortOrder));
        }

        int idx = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            log.warn("moveSortOrder: category id={} not found in siblings, parentId={}", id, parentId);
            return;
        }

        int targetIdx = "up".equals(direction) ? idx - 1 : idx + 1;
        if (targetIdx < 0 || targetIdx >= siblings.size()) return;

        Integer tmp = siblings.get(idx).getSortOrder();
        siblings.get(idx).setSortOrder(siblings.get(targetIdx).getSortOrder());
        siblings.get(targetIdx).setSortOrder(tmp);

        categoryMapper.updateById(siblings.get(idx));
        categoryMapper.updateById(siblings.get(targetIdx));
    }
}
