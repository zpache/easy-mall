package com.easymall.admin.modules.dashboard.vo;

import java.math.BigDecimal;

/**
 * 分析页顶部指标卡数据。
 *
 * @param todayOrders     今日订单数
 * @param totalOrders     累计订单数
 * @param todaySales      今日销售额（按支付时间，元）
 * @param totalSales      累计销售额（已付款订单，元）
 * @param pendingDelivery 待发货订单数
 * @param finishedOrders  已完成订单数
 * @param stockWarning    库存预警商品数（库存 <= 预警阈值）
 * @param onSaleGoods     在售商品数
 */
public record DashboardOverviewVO(
        long todayOrders,
        long totalOrders,
        BigDecimal todaySales,
        BigDecimal totalSales,
        long pendingDelivery,
        long finishedOrders,
        long stockWarning,
        long onSaleGoods
) {
}
