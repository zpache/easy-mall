package cn.easykits.api.modules.goods.vo;

import cn.easykits.api.entity.Category;
import cn.easykits.api.entity.Goods;

import java.util.List;

/**
 * 首页聚合数据。
 */
public record HomeVO(List<Category> categories, List<Goods> hotGoods, List<Goods> newGoods) {
}
