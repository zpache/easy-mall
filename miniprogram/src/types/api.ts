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
  totalAmount: number
  freightAmount: number
  payAmount: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  createdAt: string
  items: OrderItem[]
}

export interface LoginUser {
  userId: number
  openid: string
  nickname: string
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
}
