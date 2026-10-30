/** 后端统一响应结构 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface Category {
  id: number
  name: string
  parentId: number
  icon?: string
  sort: number
  status: number
}

export interface Goods {
  id: number
  categoryId: number
  name: string
  subtitle?: string
  mainImage: string
  images?: string
  price: number
  originalPrice?: number
  stock: number
  sales: number
  unit?: string
  detail?: string
  status: number
  sort: number
}

export interface HomeData {
  categories: Category[]
  hotGoods: Goods[]
  newGoods: Goods[]
}

export interface CartItem {
  id: number
  goodsId: number
  name: string
  mainImage: string | null
  price: number
  stock: number
  quantity: number
  checked: number
  subtotal: number
  invalid: boolean
}

export interface Address {
  id: number
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}

export interface AddressForm {
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: number
}

export interface OrderItem {
  id: number
  goodsId: number
  goodsName: string
  goodsImage: string
  price: number
  quantity: number
  totalAmount: number
}

export interface Order {
  id: number
  orderNo: string
  status: number
  statusText: string
  /** 售后状态：0 无 / 1 退款中 / 2 已退款 / 3 售后被拒 */
  refundStatus?: number
  refundStatusText?: null | string
  totalAmount: number
  freightAmount: number
  /** 优惠券抵扣金额，未用券为 0 */
  couponAmount: number
  /** 会员等级折扣优惠金额，未享受折扣为 0 */
  memberDiscount: number
  /** 享受折扣时的会员等级名称（下单时快照） */
  memberLevelName?: null | string
  payAmount: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  /** 快递公司，未发货为 null */
  deliveryCompany?: null | string
  /** 快递单号，未发货为 null */
  deliveryNo?: null | string
  /** 发货时间，未发货为 null */
  deliveryTime?: null | string
  remark?: string
  createdAt: string
  /** 支付截止时间，仅待付款订单有值 */
  payDeadline?: null | string
  /** 距支付截止剩余秒数（服务端算好，避免手机时钟偏差），仅待付款订单有值 */
  payRemainSeconds?: null | number
  items: OrderItem[]
}

export interface LoginUser {
  userId: number
  openid: string
  nickname: string
}

/** 发起支付返回：mock=true 表示已走模拟支付并完成入账，无需再调微信 */
export interface PrepayResult {
  mock: boolean
  appId?: string
  timeStamp?: string
  nonceStr?: string
  /** 微信要求字段名就叫 package，值为 prepay_id=xxx */
  package?: string
  signType?: string
  paySign?: string
}

/** 售后申请结果 */
export interface RefundApplyResult {
  id: number
  refundNo: string
  amount: number
  status: number
  statusText: string
  appliedAt: string
}

/** 运费预览：deliverable=false 时该地区不配送，freightAmount 无效 */
export interface FreightPreview {
  freightAmount: number
  deliverable: boolean
  message?: string | null
}

/** 行政区域节点（省市区选择器数据） */
export interface RegionOption {
  areaCode: string
  name: string
  level: number
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: LoginUser
}

export interface UserProfile {
  id: number
  openid: string
  nickname: string
  avatar?: string
  phone?: string
  gender: number
  /** 会员等级 ID 与名称（名称由服务端拼好，仅展示用） */
  levelId?: number
  levelName?: string
  /** 等级折扣百分比（100 表示无折扣），用于结算页本地估算会员优惠 */
  levelDiscount?: number
  /** 当前积分 */
  points?: number
  /** 累计消费（已付款订单实付合计） */
  totalSpend?: number
  /** 累计订单数（已付款订单） */
  orderCount?: number
}
