import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OrderCommentApi {
  export interface Comment {
    [key: string]: any;
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
  changeCommentVisible,
  deleteComment,
  getCommentList,
  replyComment,
};
