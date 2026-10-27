import { requestClient } from '#/api/request';

/** 分析页顶部指标数据 */
export interface DashboardOverview {
  /** 今日订单数 */
  todayOrders: number;
  /** 订单较昨日环比（%） */
  ordersChainRate: number;
  /** 今日销售额（元） */
  todaySales: number;
  /** 销售额较昨日环比（%） */
  salesChainRate: number;
  /** 待发货订单数 */
  pendingDelivery: number;
  /** 已完成订单数 */
  finishedOrders: number;
  /** 库存预警商品数 */
  stockWarning: number;
  /** 在售商品数 */
  onSaleGoods: number;
}

/** 获取分析页顶部 4 个指标卡数据 */
export async function getDashboardOverviewApi() {
  return requestClient.get<DashboardOverview>('/dashboard/overview');
}
