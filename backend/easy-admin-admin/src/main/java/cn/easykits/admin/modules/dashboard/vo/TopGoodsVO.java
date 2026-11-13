package cn.easykits.admin.modules.dashboard.vo;

import java.math.BigDecimal;

/**
 * 商品销量排行单条：按订单明细快照聚合（排除已取消订单）。
 */
public record TopGoodsVO(String goodsName, long quantity, BigDecimal salesAmount) {
}
