import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OrderRefundApi {
  export interface Refund {
    [key: string]: any;
    amount: number;
    appliedAt: string;
    auditRemark?: string;
    handledAt?: string;
    id: number;
    orderCreatedAt?: string;
    orderId: number;
    orderNo: string;
    orderStatus?: number;
    reason?: string;
    refundNo: string;
    status: 0 | 1 | 2 | 3;
    type: 'refund' | 'return';
    userId: number;
    userNickname?: string;
  }

  export interface AuditPayload {
    pass: boolean;
    remark?: string;
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
 * 售后单分页列表
 */
async function getRefundList(params: Recordable<any>) {
  const result = await requestClient.get<
    OrderRefundApi.PageResult<OrderRefundApi.Refund>
  >('/refunds', { params });
  return { items: result.records, total: result.total };
}

/**
 * 审核售后单（同意 / 拒绝）
 */
async function auditRefund(id: number, data: OrderRefundApi.AuditPayload) {
  return requestClient.put(`/refunds/${id}/audit`, data);
}

/**
 * 确认退款（打款完成）
 */
async function confirmRefund(id: number) {
  return requestClient.put(`/refunds/${id}/confirm`);
}

export { auditRefund, confirmRefund, getRefundList };
