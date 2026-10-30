import { buildQuery, request } from './request'
import type { FreightPreview, LogisticsTrace, Order, PageResult, PrepayResult, PromotionPreview, RefundApplyResult } from '@/types/api'
import type { UsableCoupon } from '@/types/coupon'

export const orderApi = {
  /** 提交订单（购物车已勾选商品），couponUserId 为选用的会员券记录 id */
  create: (addressId: number, remark?: string, couponUserId?: number) =>
    request<Order>({
      url: '/orders',
      method: 'POST',
      data: { addressId, remark, couponUserId },
      loading: true
    }),

  /** 立即购买下单：不走购物车，不影响购物车数据 */
  buyNow: (data: { goodsId: number, quantity: number, addressId: number, remark?: string, couponUserId?: number }) =>
    request<Order>({ url: '/orders/buy-now', method: 'POST', data, loading: true }),

  /** 结算页可用优惠券：默认按购物车已勾选商品计算；立即购买时按 goodsId+quantity 计算 */
  usableCoupons: (params?: { goodsId?: number, quantity?: number }) =>
    request<UsableCoupon[]>({ url: `/orders/coupons${buildQuery(params ?? {})}` }),

  /** 结算页满减预估：口径同可用优惠券，usable=false 表示未达门槛 */
  promotionPreview: (params?: { goodsId?: number, quantity?: number }) =>
    request<PromotionPreview[]>({ url: `/orders/promotions${buildQuery(params ?? {})}` }),

  /** 运费预览：按收货地址与商品总额，deliverable=false 表示该地区不配送 */
  freightPreview: (addressId: number, totalAmount: number) =>
    request<FreightPreview>({ url: `/freight/preview${buildQuery({ addressId, totalAmount })}` }),

  list: (status?: number, pageNum = 1, pageSize = 10) =>
    request<PageResult<Order>>({ url: `/orders${buildQuery({ status, pageNum, pageSize })}` }),

  detail: (id: number) => request<Order>({ url: `/orders/${id}` }),

  /** 物流轨迹（快递100 实时查询，消耗查询额度，按需调用） */
  logistics: (id: number) => request<LogisticsTrace>({ url: `/orders/${id}/logistics` }),

  /** 模拟支付，真实项目替换为微信支付 */
  pay: (id: number) => request<void>({ url: `/orders/${id}/pay`, method: 'PUT', loading: true }),

  /**
   * 发起支付：启用微信支付时返回调起支付参数；未启用但开启模拟支付时后端直接完成入账，
   * 返回 mock=true。前端只需判断 mock 决定是否调 wx.requestPayment。
   */
  prepay: (orderId: number) =>
    request<PrepayResult>({
      url: `/pay/wxpay/prepay${buildQuery({ orderId })}`,
      method: 'POST',
      loading: true
    }),

  cancel: (id: number) => request<void>({ url: `/orders/${id}/cancel`, method: 'PUT', loading: true }),

  /** 确认收货：待收货 → 已完成 */
  confirmReceive: (id: number) =>
    request<void>({ url: `/orders/${id}/receive`, method: 'PUT', loading: true }),

  /** 申请退款：金额由服务端按「实付 − 已退」计算 */
  applyRefund: (data: { orderId: number, reason?: string, type?: string }) =>
    request<RefundApplyResult>({ url: '/refunds/apply', method: 'POST', data, loading: true })
}
