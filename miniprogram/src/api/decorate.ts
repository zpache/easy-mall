import { buildQuery, request } from './request'
import type { CouponCard } from '@/types/coupon'
import type { DecoratePageData, NavConfig } from '@/types/decorate'

interface RawNav<A, B, C> {
  tabbarJson?: A
  navbarJson?: B
  floatJson?: C
}

export const decorateApi = {
  /** 商城首页（页面结构 + 导航配置） */
  home: () => request<DecoratePageData>({ url: '/decorate/home' }),

  /** 微页面（页面结构 + 导航配置） */
  page: (id: number | string) => request<DecoratePageData>({ url: `/decorate/page/${id}` }),

  /** 全局导航配置（三段 JSON 字符串） */
  nav: () => request<RawNav<string, string, string>>({ url: '/decorate/nav' }),

  /** 装修「优惠券」组件的券卡片，按传入 id 顺序返回（免登录） */
  coupons: (ids: number[]) =>
    request<CouponCard[]>({ url: `/decorate/coupons${buildQuery({ ids: ids.join(',') })}` })
}

export type { NavConfig }
