import { request } from './request'

/** 已启用的订阅消息模板 ID（未启用/未配置返回空数组） */
export const notifyApi = {
  subscribeTemplates: () => request<string[]>({ url: '/notify/subscribe-templates' })
}
