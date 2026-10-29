import { ref } from 'vue'
import { defineStore } from 'pinia'
import { decorateApi } from '@/api/decorate'
import { DEFAULT_THEME, parseNav, parseTheme } from '@/utils/decorate'
import type { NavConfig, ThemeConfig } from '@/types/decorate'

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

  /** 线上主题拉取：接口异常时保持默认主题，不阻断页面渲染 */
  async function ensureTheme(force = false) {
    if (themeLoaded.value && !force) return theme.value
    try {
      theme.value = parseTheme(await decorateApi.theme())
      themeLoaded.value = true
    } catch {
      // 主题接口异常时保持默认主题
    }
    return theme.value
  }

  /** 首页接口已带回导航配置，直接复用省一次请求 */
  function setNav(config: NavConfig) {
    nav.value = config
    loaded.value = true
  }

  return { nav, loaded, ensureNav, setNav, theme, themeLoaded, ensureTheme }
})
