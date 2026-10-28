import { buildQuery, request } from './request'
import type { CouponCard, MemberCoupon, MemberCouponResult } from '@/types/coupon'

export const couponApi = {
  /** 我的优惠券：status 不传返回全部，同时带上各状态数量 */
  mine: (status?: number) =>
    request<MemberCouponResult>({ url: `/coupons/mine${buildQuery({ status })}` }),

  /** 领券中心：按券的随机码领取（需登录） */
  receive: (code: string) =>
    request<MemberCoupon>({
      url: `/coupons/${encodeURIComponent(code)}/receive`,
      method: 'POST'
    })
}

export type { CouponCard }
