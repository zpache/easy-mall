import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useDecorateStore } from '@/store/decorate'
import type { ThemeConfig } from '@/types/decorate'

/** 主题配置键 → 注入到页面的 CSS 变量名（与 uni.scss 的默认值一一对应） */
export const THEME_VAR_MAP: ReadonlyArray<readonly [string, keyof ThemeConfig]> = [
  ['--d-primary', 'primaryColor'],
  ['--d-page-bg', 'pageBgColor'],
  ['--d-price', 'priceColor'],
  ['--d-text', 'textColor'],
  ['--d-sub-text', 'subTextColor']
]

/**
 * 线上主题读取 + 注入：
 * 1. `theme` 响应式主题对象，需要动态算色的场景直接读；
 * 2. `themeVars` 变量对象，给普通 view 根节点绑定 :style 用；
 * 3. `themePageStyle` 变量字符串，给 <page-meta page-style> 用（小程序端推荐，
 *    挂在页面根节点上，page 标签与所有子节点都能继承，页面底色也会跟着变）；
 * 4. 进入页面时按 store 的 TTL 校验一次线上主题，后台「启用主题」后
 *    切换页面 / 小程序回前台即生效，无需重启小程序。
 */
export function useTheme() {
  const decorateStore = useDecorateStore()
  const { theme } = storeToRefs(decorateStore)

  const themeVars = computed<Record<string, string>>(() => {
    const vars: Record<string, string> = {}
    for (const [name, key] of THEME_VAR_MAP) vars[name] = theme.value[key]
    return vars
  })

  const themePageStyle = computed(() => {
    const current = theme.value
    return THEME_VAR_MAP.map(([name, key]) => `${name}: ${current[key]}`).join('; ')
  })

  // 非阻塞：TTL 内直接复用缓存，过期或后台刚改过才真正发请求
  void decorateStore.ensureTheme()

  return {
    theme,
    themeVars,
    themePageStyle,
    refreshTheme: () => decorateStore.ensureTheme(true)
  }
}
