import type { ApiResult } from '@/types/api'
import { clearToken, getToken } from '@/utils/storage'

const BASE_URL = import.meta.env.VITE_API_BASE_URL

/** 接口根地址，上传等非 request 封装的场景复用 */
export const API_BASE_URL = BASE_URL

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, any>
  /** 是否显示 loading，默认 false */
  loading?: boolean
  /** 401 时是否自动跳转登录，默认 true */
  redirectOnUnauthorized?: boolean
}

/** 业务异常，页面可 catch 后做差异化处理 */
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

let redirecting = false

function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  clearToken()
  uni.navigateTo({
    url: '/pages/login/index',
    complete: () => {
      setTimeout(() => {
        redirecting = false
      }, 800)
    }
  })
}

function toast(message: string) {
  uni.showToast({ title: message || '请求失败', icon: 'none' })
}

/** 后端成功码是 0（兼容历史 200） */
function isSuccess(code: number | undefined) {
  return code === 0 || code === 200
}

/**
 * 统一请求封装：
 * 1. 自动拼接 baseURL、注入 Bearer token
 * 2. 统一处理 HTTP 401 与业务 code
 * 3. 直接返回 data，页面不用关心外层包装
 */
export function request<T>(options: RequestOptions): Promise<T> {
  const { url, method = 'GET', data, loading = false, redirectOnUnauthorized = true } = options
  const token = getToken()

  if (loading) {
    uni.showLoading({ title: '加载中', mask: true })
  }

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        const body = res.data as ApiResult<T>

        if (res.statusCode === 401 || body?.code === 401) {
          if (redirectOnUnauthorized) {
            redirectToLogin()
          }
          reject(new ApiError(401, body?.message || '登录已失效'))
          return
        }

        if (res.statusCode >= 200 && res.statusCode < 300 && isSuccess(body?.code)) {
          resolve(body.data)
          return
        }

        const message = body?.message || `请求失败(${res.statusCode})`
        toast(message)
        reject(new ApiError(body?.code ?? res.statusCode, message))
      },
      fail: (err) => {
        toast('网络异常，请检查网络后重试')
        reject(new ApiError(-1, err.errMsg || '网络异常'))
      },
      complete: () => {
        if (loading) {
          uni.hideLoading()
        }
      }
    })
  })
}

/** 拼装 query string，过滤空值 */
export function buildQuery(params: Record<string, any>): string {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return query ? `?${query}` : ''
}
