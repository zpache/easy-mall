import { request } from './request'
import type { Address, AddressForm, LoginResult, UserProfile } from '@/types/api'

export const authApi = {
  /** 微信 code 换 token；inviterId 仅在新用户注册时生效（分享邀请归因） */
  login: (code: string, inviterId?: number) =>
    request<LoginResult>({
      url: '/auth/login',
      method: 'POST',
      data: inviterId ? { code, inviterId } : { code }
    }),

  logout: () => request<void>({ url: '/auth/logout', method: 'POST', redirectOnUnauthorized: false })
}

export interface InviteRecord {
  id: number
  nickname: string
  avatar: string
  /** 0 已注册未下单 1 已完成首单 */
  status: number
  registerPoints: number
  orderPoints: number
  createdAt: string
  orderedAt?: string
}

export interface InviteSummary {
  invitedCount: number
  rewardPoints: number
  records: { records: InviteRecord[]; total: number; pageNum: number; pageSize: number; pages: number }
}

export const inviteApi = {
  /** 我的邀请汇总 + 邀请记录分页 */
  records: (pageNum = 1, pageSize = 20) =>
    request<InviteSummary>({ url: '/member/invite/records', data: { pageNum, pageSize } })
}

export const userApi = {
  profile: () => request<UserProfile>({ url: '/user/profile' }),

  updateProfile: (data: { nickname?: string; avatar?: string; gender?: number }) =>
    request<UserProfile>({ url: '/user/profile', method: 'PUT', data })
}

export const addressApi = {
  list: () => request<Address[]>({ url: '/addresses' }),

  detail: (id: number) => request<Address>({ url: `/addresses/${id}` }),

  create: (data: AddressForm) => request<Address>({ url: '/addresses', method: 'POST', data }),

  update: (id: number, data: AddressForm) =>
    request<Address>({ url: `/addresses/${id}`, method: 'PUT', data }),

  remove: (id: number) => request<void>({ url: `/addresses/${id}`, method: 'DELETE' }),

  setDefault: (id: number) => request<void>({ url: `/addresses/${id}/default`, method: 'PUT' })
}
