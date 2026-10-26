import { buildQuery, request } from './request'
import type { Category, Goods, HomeData, PageResult } from '@/types/api'

export interface GoodsQuery {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  keyword?: string
  sortBy?: 'sales' | 'priceAsc' | 'priceDesc' | 'new'
}

export const goodsApi = {
  /** 首页聚合数据（分类 + 热销 + 新品） */
  home: () => request<HomeData>({ url: '/home' }),

  /** 分类列表 */
  categories: () => request<Category[]>({ url: '/categories' }),

  /** 商品列表 */
  list: (query: GoodsQuery = {}) =>
    request<PageResult<Goods>>({ url: `/goods${buildQuery(query)}` }),

  /** 商品详情 */
  detail: (id: number | string) => request<Goods>({ url: `/goods/${id}` })
}
