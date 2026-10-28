import { API_BASE_URL, ApiError } from './request'
import type { ApiResult } from '@/types/api'
import { clearToken, getToken } from '@/utils/storage'

interface UploadResult {
  url: string
}

function toast(message: string) {
  uni.showToast({ title: message, icon: 'none' })
}

function parseBody(raw: unknown): ApiResult<UploadResult> | undefined {
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as ApiResult<UploadResult>
    } catch {
      return undefined
    }
  }
  return raw as ApiResult<UploadResult>
}

/**
 * 上传头像，返回后端可访问的绝对地址。
 * uni.request 无法传文件，这里单独用 uni.uploadFile 并复用 token。
 */
export function uploadAvatar(filePath: string): Promise<string> {
  const token = getToken()
  uni.showLoading({ title: '上传中', mask: true })
  return new Promise<string>((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/uploads/avatar`,
      filePath,
      name: 'file',
      timeout: 30000,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        const body = parseBody(res.data)
        if (res.statusCode === 401 || body?.code === 401) {
          clearToken()
          reject(new ApiError(401, '登录已失效，请重新登录'))
          return
        }
        const ok = res.statusCode >= 200 && res.statusCode < 300
          && (body?.code === 0 || body?.code === 200)
        if (ok && body?.data?.url) {
          resolve(body.data.url)
          return
        }
        const message = body?.message || `头像上传失败(${res.statusCode})`
        toast(message)
        reject(new ApiError(body?.code ?? res.statusCode, message))
      },
      fail: (err) => {
        toast('头像上传失败，请检查网络')
        reject(new ApiError(-1, err.errMsg || '头像上传失败'))
      },
      complete: () => {
        uni.hideLoading()
      }
    })
  })
}
