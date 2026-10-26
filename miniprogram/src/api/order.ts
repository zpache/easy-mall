import { buildQuery, request } from './request'
import type { Order, PageResult } from '@/types/api'

export const orderApi = {
  /** 提交订单（购物车已勾选商品） */
  create: (addressId: number, remark?: string) =>
    request<Order>({ url: '/orders', method: 'POST', data: { addressId, remark }, loading: true }),

  list: (status?: number, pageNum = 1, pageSize = 10) =>
    request<PageResult<Order>>({ url: `/orders${buildQuery({ status, pageNum, pageSize })}` }),

  detail: (id: number) => request<Order>({ url: `/orders/${id}` }),

  /** 模拟支付，真实项目替换为微信支付 */
  pay: (id: number) => request<void>({ url: `/orders/${id}/pay`, method: 'PUT', loading: true }),

  cancel: (id: number) => request<void>({ url: `/orders/${id}/cancel`, method: 'PUT', loading: true })
}
