import { request } from './request'
import type { FavoriteItem, PageResult } from '@/types/api'

export const favoriteApi = {
  /** 切换收藏（收藏/取消收藏），返回切换后是否已收藏 */
  toggle: (goodsId: number) =>
    request<boolean>({ url: `/favorites/${goodsId}/toggle`, method: 'POST' }),

  /** 商品是否已收藏（详情页心形按钮初始态） */
  status: (goodsId: number) => request<boolean>({ url: `/favorites/${goodsId}/status` }),

  /** 收藏分页列表（按收藏时间倒序） */
  list: (pageNum = 1, pageSize = 10) =>
    request<PageResult<FavoriteItem>>({ url: `/favorites${buildPage(pageNum, pageSize)}` })
}

function buildPage(pageNum: number, pageSize: number) {
  return `?pageNum=${pageNum}&pageSize=${pageSize}`
}
