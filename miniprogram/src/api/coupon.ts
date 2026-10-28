import { buildQuery, request } from './request'
import type { MemberCouponResult } from '@/types/coupon'

export const couponApi = {
  /** 我的优惠券：status 不传返回全部，同时带上各状态数量 */
  mine: (status?: number) =>
    request<MemberCouponResult>({ url: `/coupons/mine${buildQuery({ status })}` })
}
