import { buildQuery, request } from './request'
import type { CouponCard, MemberCoupon, MemberCouponResult } from '@/types/coupon'

export const couponApi = {
  /** 我的优惠券：status 不传返回全部，同时带上各状态数量 */
  mine: (status?: number) =>
    request<MemberCouponResult>({ url: `/coupons/mine${buildQuery({ status })}` }),

  /** 领券中心：领取指定优惠券（需登录） */
  receive: (couponId: number) =>
    request<MemberCoupon>({ url: `/coupons/${couponId}/receive`, method: 'POST' })
}

export type { CouponCard }
