package com.easymall.admin.modules.dashboard.vo;

import java.math.BigDecimal;

/**
 * 订单趋势单点：某日的订单数与销售额（按下单时间，排除已取消）。
 */
public record TrendPointVO(String date, long orders, BigDecimal salesAmount) {
}
