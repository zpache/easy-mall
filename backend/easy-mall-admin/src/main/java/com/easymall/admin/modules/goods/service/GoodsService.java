package com.easymall.admin.modules.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.modules.goods.dto.GoodsForm;
import com.easymall.admin.modules.goods.entity.Category;
import com.easymall.admin.modules.goods.entity.Goods;
import com.easymall.admin.modules.goods.mapper.CategoryMapper;
import com.easymall.admin.modules.goods.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 商品服务。
 */
@Service
@RequiredArgsConstructor
public class GoodsService extends ServiceImpl<GoodsMapper, Goods> {

    private final CategoryMapper categoryMapper;

    public PageResult<Goods> pageQuery(long pageNum, long pageSize, String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<Goods> wrapper = Wrappers.<Goods>lambdaQuery()
                .like(StringUtils.hasText(keyword), Goods::getName, keyword)
                .eq(categoryId != null, Goods::getCategoryId, categoryId)
                .eq(status != null, Goods::getStatus, status)
                .orderByDesc(Goods::getId);
        Page<Goods> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public Goods detail(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return goods;
    }

    public Goods create(GoodsForm form) {
        checkCategory(form.categoryId());
        Goods goods = new Goods();
        applyForm(goods, form);
        goods.setSales(0);
        save(goods);
        return goods;
    }

    public Goods update(Long id, GoodsForm form) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        checkCategory(form.categoryId());
        applyForm(goods, form);
        if (form.version() != null) {
            goods.setVersion(form.version());
        }
        if (!updateById(goods)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return goods;
    }

    /**
     * 上架/下架。
     */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（下架）或 1（上架）");
        }
        Goods goods = getById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        goods.setStatus(status);
        if (!updateById(goods)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    private void checkCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
    }

    private void applyForm(Goods goods, GoodsForm form) {
        goods.setCategoryId(form.categoryId());
        goods.setName(form.name());
        goods.setSubtitle(form.subtitle());
        goods.setMainImage(form.mainImage());
        goods.setImages(form.images());
        goods.setPrice(form.price());
        goods.setOriginalPrice(form.originalPrice());
        goods.setStock(form.stock());
        goods.setUnit(form.unit());
        goods.setDetail(form.detail());
        goods.setStatus(form.status());
        goods.setSort(form.sort() == null ? 0 : form.sort());
    }
}
