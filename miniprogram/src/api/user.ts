import { request } from './request'
import type { Address, AddressForm, LoginResult, UserProfile } from '@/types/api'

export const authApi = {
  /** 微信 code 换 token */
  login: (code: string) =>
    request<LoginResult>({ url: '/auth/login', method: 'POST', data: { code } }),

  logout: () => request<void>({ url: '/auth/logout', method: 'POST', redirectOnUnauthorized: false })
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
