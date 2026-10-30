package com.easymall.admin.modules.goods.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Category;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.mapper.CategoryMapper;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.modules.goods.dto.CategoryForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务。
 */
@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    private final GoodsMapper goodsMapper;

    public List<Category> listAll() {
        return list(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    /**
     * 分类树（分类管理页与下拉树使用）。
     */
    public List<Category> listTree() {
        List<Category> all = listAll();
        Map<Long, List<Category>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(category -> category.getParentId() == null ? 0L : category.getParentId()));
        all.forEach(category -> category.setChildren(
                childrenMap.getOrDefault(category.getId(), List.of())));
        return childrenMap.getOrDefault(0L, List.of());
    }

    public Category create(CategoryForm form) {
        Category category = new Category();
        applyForm(category, form);
        save(category);
        return category;
    }

    public Category update(Long id, CategoryForm form) {
        Category category = getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (id.equals(form.parentId())) {
            throw new BusinessException("上级分类不能是自己");
        }
        applyForm(category, form);
        if (form.version() != null) {
            category.setVersion(form.version());
        }
        if (!updateById(category)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return category;
    }

    public void delete(Long id) {
        long children = count(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        if (children > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "存在子分类，无法删除");
        }
        Long goodsCount = goodsMapper.selectCount(Wrappers.<Goods>lambdaQuery().eq(Goods::getCategoryId, id));
        if (goodsCount != null && goodsCount > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "分类下存在商品，无法删除");
        }
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    private void applyForm(Category category, CategoryForm form) {
        category.setName(form.name());
        category.setParentId(form.parentId() == null ? 0L : form.parentId());
        category.setSort(form.sort() == null ? 0 : form.sort());
        category.setStatus(form.status());
    }
}
