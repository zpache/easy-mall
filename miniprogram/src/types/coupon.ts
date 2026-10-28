/** 我的优惠券（与后端 MemberCouponVO 对应） */
export interface MemberCoupon {
  /** 领券记录 id */
  id: number
  couponId: number
  name: string
  /** 1 满减券 2 折扣券 3 无门槛券 */
  type: number
  amount: number | null
  discount: number | null
  threshold: number
  /** 归一后状态：0 可用 1 已使用 2 已过期 */
  status: number
  /** 面额文案，如 ¥15 / 9折 */
  valueText: string
  /** 门槛文案，如 满99元可用 / 无门槛 */
  ruleText: string
  orderNo: string | null
  receivedAt: string | null
  usedAt: string | null
  expireAt: string | null
}

export interface MemberCouponResult {
  available: number
  used: number
  expired: number
  list: MemberCoupon[]
}
