import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OrderCommentApi {
  /** 审核状态：0 待审核，1 已通过，2 已拒绝 */
  export type AuditStatus = 0 | 1 | 2;

  export interface Comment {
    [key: string]: any;
    auditAt?: string;
    auditRemark?: string;
    auditStatus: AuditStatus;
    commentedAt: string;
    content?: string;
    goodsId: number;
    goodsImage?: string;
    goodsName: string;
    id: number;
    images?: string;
    isShow: 0 | 1;
    orderId: number;
    orderNo: string;
    repliedAt?: string;
    reply?: string;
    score: number;
    userId: number;
    userNickname?: string;
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
 * 评价分页列表
 */
async function getCommentList(params: Recordable<any>) {
  const result = await requestClient.get<
    OrderCommentApi.PageResult<OrderCommentApi.Comment>
  >('/comments', { params });
  return { items: result.records, total: result.total };
}

/**
 * 待审核评价数量
 */
async function getPendingCommentCount() {
  return requestClient.get<number>('/comments/pending-count');
}

/**
 * 审核评价
 */
async function auditComment(
  id: number,
  status: OrderCommentApi.AuditStatus,
  remark?: string,
) {
  return requestClient.put(`/comments/${id}/audit`, { remark, status });
}

/**
 * 显示 / 隐藏评价
 */
async function changeCommentVisible(id: number, isShow: number) {
  return requestClient.put(`/comments/${id}/visible`, null, {
    params: { isShow },
  });
}

/**
 * 回复评价
 */
async function replyComment(id: number, reply: string) {
  return requestClient.put(`/comments/${id}/reply`, { reply });
}

/**
 * 删除评价
 */
async function deleteComment(id: number) {
  return requestClient.delete(`/comments/${id}`);
}

export {
  auditComment,
  changeCommentVisible,
  deleteComment,
  getCommentList,
  getPendingCommentCount,
  replyComment,
};
