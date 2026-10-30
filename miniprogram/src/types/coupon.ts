/** 我的优惠券（与后端 MemberCouponVO 对应） */
export interface MemberCoupon {
  /** 领券记录 id */
  id: number
  /** 券的对外随机码（后端不再下发自增主键） */
  couponCode: string
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

/** 领券中心的券卡片（与后端 CouponCardVO 对应） */
export interface CouponCard {
  /** 券的对外随机码，领取时作为路径参数 */
  code: string
  name: string
  /** 1 满减券 2 折扣券 3 无门槛券 */
  type: number
  /** 面额文案，如 ¥15 / 9折 */
  valueText: string
  /** 门槛文案，如 满99元可用 / 无门槛 */
  ruleText: string
  /** 有效期文案，如 领取后7天有效 / 2026.10.01-2026.11.11 */
  validText: string
  totalCount: number
  /** 剩余张数，-1 表示不限量 */
  remainCount: number
  /** 每人限领张数 */
  perLimit: number
  soldOut: boolean
}

/** 结算页可用优惠券（与后端 UsableCouponVO 对应，服务端按购物车金额实时计算） */
export interface UsableCoupon {
  /** 领券记录 id，下单时回传 */
  couponUserId: number
  name: string
  /** 面额文案，如 ¥15 / 9折 */
  valueText: string
  /** 门槛文案，如 满99元可用 / 无门槛 */
  ruleText: string
  /** 本单可抵扣金额，不可用为 0 */
  discountAmount: number
  /** 是否可用 */
  usable: boolean
  /** 不可用原因（还差 X 元可用 / 已过有效期） */
  reason: string | null
  /** 到期日期文案，如 2026.11.11，null 表示长期有效 */
  expireAt: string | null
}
