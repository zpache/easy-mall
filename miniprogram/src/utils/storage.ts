const TOKEN_KEY = 'easy_mall_token'
const UID_KEY = 'easy_mall_uid'

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken(): void {
  uni.removeStorageSync(TOKEN_KEY)
}

/**
 * 当前登录用户 ID：分享链接要带上它做邀请归因。
 * 单独缓存一份，避免分享工具依赖 pinia 实例（启动早期 / 后台唤起时 store 可能还没就绪）。
 */
export function getUid(): number {
  return Number(uni.getStorageSync(UID_KEY)) || 0
}

export function setUid(uid?: number): void {
  if (uid && uid > 0) uni.setStorageSync(UID_KEY, uid)
}

export function clearUid(): void {
  uni.removeStorageSync(UID_KEY)
}
