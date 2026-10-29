import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api/user'
import type { UserProfile } from '@/types/api'
import { clearToken, getToken, setToken } from '@/utils/storage'

/**
 * 用户状态：token + 资料。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const profile = ref<UserProfile | null>(null)
  const isLogin = computed(() => !!token.value)

  /** 微信静默登录：wx.login 拿 code -> 后端换 token */
  async function login() {
    const code = await getWxCode()
    const result = await authApi.login(code)
    token.value = result.accessToken
    setToken(result.accessToken)
    await fetchProfile()
  }

  async function fetchProfile() {
    profile.value = await userApi.profile()
    return profile.value
  }

  /** 资料页保存成功后本地同步，省一次查询 */
  function setProfile(value: UserProfile) {
    profile.value = value
  }

  /** 微信不再下发昵称头像，这里判断是否还需要用户补全 */
  const profileIncomplete = computed(() => {
    if (!profile.value) return false
    const { avatar, nickname } = profile.value
    return !avatar || !nickname || nickname === '微信用户'
  })

  async function logout() {
    // 先通知服务端销毁登录态（Redis 会话），本地无论成败都清空
    await authApi.logout().catch(() => undefined)
    token.value = ''
    profile.value = null
    clearToken()
  }

  let relogining = false
  /**
   * 静默重登：wx.login 不需要用户确认，token 失效（服务端会话过期/被踢）时无感换新。
   * 返回 true 表示已拿到新 token。
   */
  async function silentRelogin(): Promise<boolean> {
    if (relogining) return false
    relogining = true
    try {
      await login()
      return true
    } catch {
      return false
    } finally {
      relogining = false
    }
  }

  return { token, profile, isLogin, profileIncomplete, login, fetchProfile, setProfile, silentRelogin, logout }
})

/**
 * 获取微信登录 code。H5 调试时 uni.login 不可用，用时间戳兜底
 * （后端 easy-mall.wx.mock=true 时会直接接受任意 code）。
 */
function getWxCode(): Promise<string> {
  return new Promise((resolve) => {
    uni.login({
      provider: 'weixin',
      success: (res) => resolve(res.code || `h5_${Date.now()}`),
      fail: () => resolve(`h5_${Date.now()}`)
    })
  })
}
