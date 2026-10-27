package com.easymall.admin.modules.dashboard.vo;

import java.math.BigDecimal;

/**
 * 分析页顶部指标卡数据。
 *
 * @param todayOrders     今日订单数
 * @param ordersChainRate 订单较昨日环比（%），昨日为 0 且今日有单记 +100，两者皆无记 0
 * @param todaySales      今日销售额（按支付时间，元）
 * @param salesChainRate  销售额较昨日环比（%），口径同上
 * @param pendingDelivery 待发货订单数
 * @param finishedOrders  已完成订单数
 * @param stockWarning    库存预警商品数（库存 <= 预警阈值）
 * @param onSaleGoods     在售商品数
 */
public record DashboardOverviewVO(
        long todayOrders,
        BigDecimal ordersChainRate,
        BigDecimal todaySales,
        BigDecimal salesChainRate,
        long pendingDelivery,
        long finishedOrders,
        long stockWarning,
        long onSaleGoods
) {
}
