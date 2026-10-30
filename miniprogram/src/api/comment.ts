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

  /** 商品评价列表与评分概览（免登录，游客也能看） */
  goodsComments: (goodsId: number, pageNum = 1, pageSize = 10) =>
    request<GoodsCommentPage>({
      url: `/comments/goods/${goodsId}${buildQuery({ pageNum, pageSize })}`
    })
}
