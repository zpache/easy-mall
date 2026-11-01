import { requestClient } from '#/api/request';

export namespace DashboardAnalyticsApi {
  export interface MemberGrowthPoint {
    date: string;
    newMembers: number;
  }

  export interface TopGoods {
    goodsName: string;
    quantity: number;
    salesAmount: number;
  }

  export interface TrendPoint {
    date: string;
    orders: number;
    salesAmount: number;
  }
}

/**
 * 订单趋势（近 N 天，含无单日期补零）
 */
async function getOrderTrend(days = 30) {
  return requestClient.get<DashboardAnalyticsApi.TrendPoint[]>(
    '/dashboard/order-trend',
    { params: { days } },
  );
}

/**
 * 商品销量排行（近 N 天，按销量降序）
 */
async function getTopGoods(days = 30, limit = 10) {
  return requestClient.get<DashboardAnalyticsApi.TopGoods[]>(
    '/dashboard/top-goods',
    { params: { days, limit } },
  );
}

/**
 * 会员增长（近 N 天，含无注册日期补零）
 */
async function getMemberGrowth(days = 30) {
  return requestClient.get<DashboardAnalyticsApi.MemberGrowthPoint[]>(
    '/dashboard/member-growth',
    { params: { days } },
  );
}

export { getMemberGrowth, getOrderTrend, getTopGoods };
