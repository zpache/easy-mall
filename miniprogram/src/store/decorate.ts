import { ref } from 'vue'
import { defineStore } from 'pinia'
import { decorateApi } from '@/api/decorate'
import { DEFAULT_THEME, parseNav, parseTheme } from '@/utils/decorate'
import type { NavConfig, ThemeConfig } from '@/types/decorate'

/**
 * 主题缓存有效期：后台「启用主题」后，小程序切页面 / 回前台最多这么久就会同步到新配色。
 * 取得短一点是为了「实时联动」，同时避免每次切页面都真的发请求。
 */
const THEME_TTL = 15 * 1000

/**
 * 装修配置：全局导航（TabBar / 顶部导航 / 悬浮球）与线上主题。
 * 各页面共用一份，首次进入时拉取，失败时退回内置默认值。
 */
export const useDecorateStore = defineStore('decorate', () => {
  const nav = ref<NavConfig>(parseNav(null))
  const loaded = ref(false)

  /** 线上主题：换肤用，装修组件通过 CSS 变量消费 */
  const theme = ref<ThemeConfig>({ ...DEFAULT_THEME })
  const themeLoaded = ref(false)

  /** 上次主题拉取时间（毫秒）与在途请求，用于 TTL 判定和并发去重 */
  let themeFetchedAt = 0
  let themePending: Promise<ThemeConfig> | null = null

  async function ensureNav(force = false) {
    if (loaded.value && !force) return nav.value
    try {
      nav.value = parseNav(await decorateApi.nav())
      loaded.value = true
    } catch {
      // 导航接口异常时保持默认配置，不阻断页面渲染
    }
    return nav.value
  }

  /**
   * 线上主题拉取（TTL 内复用缓存，force 立即拉取，并发请求合并）：
   * 接口异常时保持现有主题，不阻断页面渲染。
   */
  async function ensureTheme(force = false) {
    const fresh = themeLoaded.value && Date.now() - themeFetchedAt < THEME_TTL
    if (fresh && !force) return theme.value
    if (themePending) return themePending
    themePending = (async () => {
      try {
        theme.value = parseTheme(await decorateApi.theme())
        themeLoaded.value = true
      } catch {
        // 主题接口异常时保持现有主题；同样记一次时间，避免连续失败时反复重试
      } finally {
        themeFetchedAt = Date.now()
      }
      return theme.value
    })()
    try {
      return await themePending
    } finally {
      themePending = null
    }
  }

  /** 首页接口已带回导航配置，直接复用省一次请求 */
  function setNav(config: NavConfig) {
    nav.value = config
    loaded.value = true
  }

  return { nav, loaded, ensureNav, setNav, theme, themeLoaded, ensureTheme }
})
