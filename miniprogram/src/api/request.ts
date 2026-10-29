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

/**
 * 401 时的自动重新登录钩子（App 启动时注册为静默 wx.login）。
 * 返回 true 表示已拿到新 token，请求可安全重试一次。
 */
type UnauthorizedHandler = () => Promise<boolean>
let unauthorizedHandler: UnauthorizedHandler | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler
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
export interface RequestOptionsEx extends RequestOptions {
  /** 内部标记：已因 401 静默重登重试过，避免循环 */
  __retried?: boolean
}

export async function request<T>(options: RequestOptionsEx): Promise<T> {
  const { url, method = 'GET', data, loading = false, redirectOnUnauthorized = true } = options
  const token = getToken()

  if (loading) {
    uni.showLoading({ title: '加载中', mask: true })
  }

  const result = await new Promise<{ ok: boolean; body?: ApiResult<T>; statusCode: number; errMsg?: string }>(
    (resolve) => {
      uni.request({
        url: BASE_URL + url,
        method,
        data,
        timeout: 15000,
        header: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        success: (res) =>
          resolve({ ok: true, body: res.data as ApiResult<T>, statusCode: res.statusCode }),
        fail: (err) => resolve({ ok: false, statusCode: -1, errMsg: err.errMsg })
      })
    }
  )

  if (loading) {
    uni.hideLoading()
  }

  if (!result.ok) {
    toast('网络异常，请检查网络后重试')
    throw new ApiError(-1, result.errMsg || '网络异常')
  }

  const body = result.body
  const unauthorized = result.statusCode === 401 || body?.code === 401

  if (unauthorized) {
    // 自定义登录态失效：尝试静默重登（wx.login 对用户无感）后重试一次
    if (redirectOnUnauthorized && !options.__retried && unauthorizedHandler) {
      clearToken()
      const relogin = await unauthorizedHandler().catch(() => false)
      if (relogin && getToken()) {
        return request<T>({ ...options, __retried: true, redirectOnUnauthorized: false })
      }
    }
    if (redirectOnUnauthorized) {
      redirectToLogin()
    }
    throw new ApiError(401, body?.message || '登录已失效')
  }

  if (result.statusCode >= 200 && result.statusCode < 300 && isSuccess(body?.code)) {
    return body!.data as T
  }

  const message = body?.message || `请求失败(${result.statusCode})`
  toast(message)
  throw new ApiError(body?.code ?? result.statusCode, message)
}

/** 拼装 query string，过滤空值 */
export function buildQuery(params: Record<string, any>): string {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return query ? `?${query}` : ''
}
