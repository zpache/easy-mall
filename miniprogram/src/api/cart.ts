import { buildQuery, request } from './request'
import type { CartItem } from '@/types/api'

export const cartApi = {
  list: () => request<CartItem[]>({ url: '/cart' }),

  count: () => request<number>({ url: '/cart/count' }),

  /** 加入购物车：多规格商品必须带 skuId */
  add: (goodsId: number, quantity = 1, skuId?: number) =>
    request<void>({ url: '/cart', method: 'POST', data: { goodsId, quantity, skuId } }),

  updateQuantity: (id: number, quantity: number) =>
    request<void>({ url: `/cart/${id}/quantity${buildQuery({ quantity })}`, method: 'PUT' }),

  /** id 不传表示全选/全不选 */
  updateChecked: (checked: boolean, id?: number) =>
    request<void>({
      url: `/cart/checked${buildQuery({ checked: checked ? 1 : 0, id })}`,
      method: 'PUT'
    }),

  remove: (id: number) => request<void>({ url: `/cart/${id}`, method: 'DELETE' }),

  clear: () => request<void>({ url: '/cart', method: 'DELETE' })
}
