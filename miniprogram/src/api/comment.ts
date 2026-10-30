import { buildQuery, request } from './request'
import type { CommentSubmitItem, GoodsCommentPage } from '@/types/api'

export const commentApi = {
  /** 提交评价：一次可提交订单内多个商品，返回新增条数 */
  submit: (orderId: number, items: CommentSubmitItem[]) =>
    request<number>({
      url: '/comments',
      method: 'POST',
      data: { orderId, items },
      loading: true
    }),

  /**
   * 商品评价列表与评分概览。
   * 免登录可看已通过审核的评价；带登录态时额外返回自己的待审核 / 已拒绝评价。
   * 评价是商品详情的附属内容：未登录 / 登录态过期时静默降级为游客视角，不打断商品浏览也不跳登录。
   */
  goodsComments: (goodsId: number, pageNum = 1, pageSize = 10) =>
    request<GoodsCommentPage>({
      url: `/comments/goods/${goodsId}${buildQuery({ pageNum, pageSize })}`,
      redirectOnUnauthorized: false
    })
}
