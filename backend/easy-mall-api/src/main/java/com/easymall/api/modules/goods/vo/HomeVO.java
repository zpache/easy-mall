package com.easymall.api.modules.goods.vo;

import com.easymall.api.modules.goods.entity.Category;
import com.easymall.api.modules.goods.entity.Goods;

import java.util.List;

/**
 * 首页聚合数据。
 */
public record HomeVO(List<Category> categories, List<Goods> hotGoods, List<Goods> newGoods) {
}
