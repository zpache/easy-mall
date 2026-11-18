package cn.easykits.mall.api.modules.goods.vo;

import cn.easykits.mall.api.entity.Category;
import cn.easykits.mall.api.entity.Goods;

import java.util.List;

/**
 * 首页聚合数据。
 */
public record HomeVO(List<Category> categories, List<Goods> hotGoods, List<Goods> newGoods) {
}
