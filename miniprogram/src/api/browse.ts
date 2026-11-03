import { request } from './request'
import type { BrowseItem, PageResult } from '@/types/api'

export const browseApi = {
  /** 记录一次浏览（详情页进入时调用，重复浏览只刷新时间） */
  record: (goodsId: number) =>
    request<void>({ url: `/browse/${goodsId}`, method: 'POST' }),

  /** 足迹分页列表（按最近浏览时间倒序） */
  list: (pageNum = 1, pageSize = 10) =>
    request<PageResult<BrowseItem>>({ url: `/browse?pageNum=${pageNum}&pageSize=${pageSize}` })
}
