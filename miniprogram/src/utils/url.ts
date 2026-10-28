import { API_BASE_URL } from '@/api/request'

/**
 * 把后端返回的图片地址补全为可直接加载的绝对地址。
 * 兼容三种存法：完整 http(s) 地址、/files/xxx 相对路径、空值。
 */
export function resolveMediaUrl(url?: string): string {
  if (!url) return ''
  if (/^https?:\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }
  return API_BASE_URL + (url.startsWith('/') ? url : `/${url}`)
}
