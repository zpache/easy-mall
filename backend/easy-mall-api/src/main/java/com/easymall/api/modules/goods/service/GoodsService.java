package com.easymall.api.modules.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.modules.goods.entity.Category;
import com.easymall.api.modules.goods.entity.Goods;
import com.easymall.api.modules.goods.mapper.CategoryMapper;
import com.easymall.api.modules.goods.mapper.GoodsMapper;
import com.easymall.api.modules.goods.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品浏览服务（只读，仅返回上架商品）。
 */
@Service
@RequiredArgsConstructor
public class GoodsService {

    /** 首页热销/新品展示数量 */
    private static final int HOME_LIMIT = 6;

    private final GoodsMapper goodsMapper;
    private final CategoryMapper categoryMapper;

    public PageResult<Goods> pageQuery(long pageNum, long pageSize, Long categoryId, String keyword, String sortBy) {
        LambdaQueryWrapper<Goods> wrapper = Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .eq(categoryId != null, Goods::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Goods::getName, keyword);
        applySort(wrapper, sortBy);
        Page<Goods> page = goodsMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public Goods detail(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.GOODS_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BusinessException(ResultCode.GOODS_OFF_SHELF);
        }
        return goods;
    }

    public List<Category> categories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public HomeVO home() {
        List<Category> categories = categories();
        List<Goods> hotGoods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .orderByDesc(Goods::getSales)
                .last("limit " + HOME_LIMIT));
        List<Goods> newGoods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .orderByDesc(Goods::getId)
                .last("limit " + HOME_LIMIT));
        return new HomeVO(categories, hotGoods, newGoods);
    }

    private void applySort(LambdaQueryWrapper<Goods> wrapper, String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            wrapper.orderByDesc(Goods::getSort).orderByDesc(Goods::getId);
            return;
        }
        switch (sortBy) {
            case "sales" -> wrapper.orderByDesc(Goods::getSales);
            case "priceAsc" -> wrapper.orderByAsc(Goods::getPrice);
            case "priceDesc" -> wrapper.orderByDesc(Goods::getPrice);
            default -> wrapper.orderByDesc(Goods::getId);
        }
    }
}
