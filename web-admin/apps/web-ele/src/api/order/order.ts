import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OrderApi {
  export interface OrderItem {
    goodsId: number;
    goodsImage?: string;
    goodsName: string;
    id: number;
    orderId: number;
    price: number;
    quantity: number;
    totalAmount: number;
  }

  export interface Order {
    [key: string]: any;
    cancelReason?: string;
    cancelTime?: string;
    createdAt?: string;
    deliveryCompany?: string;
    deliveryNo?: string;
    deliveryTime?: string;
    finishTime?: string;
    freightAmount: number;
    id: number;
    itemCount: number;
    items: OrderItem[];
    orderNo: string;
    payAmount: number;
    /** 支付方式：0 模拟支付 / 1 微信支付 / 2 支付宝（预留）；存量已支付订单为 null */
    payChannelNo?: null | string;
    payTime?: string;
    payType?: 0 | 1 | 2 | null;
    receiverAddress: string;
    receiverName: string;
    receiverPhone: string;
    refundStatus: 0 | 1 | 2 | 3;
    remark?: string;
    status: 0 | 1 | 2 | 3 | 4;
    totalAmount: number;
    totalQuantity: number;
    userAvatar?: string;
    userId: number;
    userNickname?: string;
  }

  export interface DeliverPayload {
    deliveryCompany: string;
    deliveryNo: string;
  }

  export interface LogisticsTrace {
    company?: string;
    companyCode?: string;
    isChecked?: boolean;
    message?: string;
    /** 快递100 状态：200 正常返回 */
    status?: string;
    state?: string;
    traces: LogisticsTraceNode[];
    trackingNo?: string;
  }

  export interface LogisticsTraceNode {
    context: string;
    ftime: string;
    location?: string;
    time?: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

/**
 * 订单分页列表
 */
async function getOrderList(params: Recordable<any>) {
  const result = await requestClient.get<
    OrderApi.PageResult<OrderApi.Order>
  >('/orders', { params });
  return { items: result.records, total: result.total };
}

/**
 * 订单详情
 */
async function getOrderDetail(id: number) {
  return requestClient.get<OrderApi.Order>(`/orders/${id}`);
}

/**
 * 订单物流轨迹（快递100 实时查询）
 */
async function getOrderLogistics(id: number) {
  return requestClient.get<OrderApi.LogisticsTrace>(`/orders/${id}/logistics`);
}

/**
 * 订单发货
 */
async function deliverOrder(id: number, data: OrderApi.DeliverPayload) {
  return requestClient.put(`/orders/${id}/deliver`, data);
}

/**
 * 关闭订单
 */
async function closeOrder(id: number, reason?: string) {
  return requestClient.put(`/orders/${id}/close`, null, {
    params: { reason },
  });
}

export {
  closeOrder,
  deliverOrder,
  getOrderDetail,
  getOrderList,
  getOrderLogistics,
};
