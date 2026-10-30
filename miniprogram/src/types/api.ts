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
  /** 该商品是否已评价（服务端按评价表标记） */
  commented?: boolean
}

/** 订单参加的满减活动快照（下单时落库，活动后续修改不影响历史订单） */
export interface OrderPromotion {
  id: number
  orderId: number
  orderNo: string
  promotionId: number
  promotionName: string
  /** 1 阶梯满减 2 每满减 */
  mode: number
  threshold: number
  benefit: number
  /** 命中次数：每满减按金额可叠加多次，阶梯为 1 */
  times: number
  discountAmount: number
}

/** 结算页满减预估：当前商品金额下每个生效活动的命中情况 */
export interface PromotionPreview {
  reductionId: number
  name: string
  mode: number
  threshold: number
  benefit: number
  times: number
  /** 预计优惠金额 */
  discount: number
  /** 是否满足门槛 */
  usable: boolean
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
  /** 满减优惠金额（多活动合计），未参加为 0 */
  reductionAmount: number
  /** 参加的满减活动快照（1 对多） */
  promotions?: OrderPromotion[]
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
  /** 待评价商品数：仅已完成订单可能大于 0 */
  pendingCommentCount?: number
}

/** 物流轨迹节点（快递100 实时查询） */
export interface LogisticsTraceNode {
  context: string
  /** 格式化时间（如 2026-10-31 12:30:00） */
  ftime?: string
  location?: string
  time?: string
}

/** 物流轨迹查询结果 */
export interface LogisticsTrace {
  company?: string
  companyCode?: string
  /** 轨迹是否已签收 */
  isChecked?: boolean
  /** 快递100 状态：200 正常返回 */
  status?: string
  state?: string
  traces: LogisticsTraceNode[]
  trackingNo?: string
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

/** 商品评价（昵称已脱敏） */
export interface GoodsComment {
  id: number
  goodsId: number
  goodsName: string
  goodsImage: string
  nickname: string
  /** 1-5 星 */
  score: number
  content?: null | string
  images: string[]
  /** 商家回复 */
  reply?: null | string
  repliedAt?: null | string
  commentedAt: string
  /** 审核状态：0 待审核 1 已通过 2 已拒绝；非 1 的评价只有本人能看到 */
  auditStatus: number
  /** 审核备注（拒绝原因），仅本人可见 */
  auditRemark?: null | string
}

/** 商品评价数据：评分概览 + 分页列表 */
export interface GoodsCommentPage {
  /** 平均分，无评价为 0 */
  avgScore: number
  /** 已通过审核的评价条数（其他人的可见数量） */
  commentCount: number
  page: PageResult<GoodsComment>
}

/** 提交评价的单个商品 */
export interface CommentSubmitItem {
  goodsId: number
  score: number
  content?: string
  images?: string[]
}
