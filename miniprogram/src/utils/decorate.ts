import type {
  DecorateComponent,
  FloatConfig,
  NavConfig,
  NavTabItem,
  NavbarConfig,
  PageSchema,
  TabbarConfig,
  ThemeConfig
} from '@/types/decorate'

/** 小程序内已注册的 tab 页（与 pages.json tabBar.list 对应） */
export const TAB_ROUTES = [
  '/pages/index/index',
  '/pages/category/index',
  '/pages/cart/index',
  '/pages/mine/index'
]

export const DEFAULT_TABBAR: TabbarConfig = {
  enabled: true,
  bgColor: '#ffffff',
  color: '#909399',
  selectedColor: '#1677ff',
  borderStyle: 'black',
  items: [
    { key: 'home', name: '首页', icon: '', selectedIcon: '', linkType: 'home', linkValue: '', visible: true },
    { key: 'category', name: '分类', icon: '', selectedIcon: '', linkType: 'path', linkValue: '/pages/category/index', visible: true },
    { key: 'cart', name: '购物车', icon: '', selectedIcon: '', linkType: 'path', linkValue: '/pages/cart/index', visible: true },
    { key: 'mine', name: '我的', icon: '', selectedIcon: '', linkType: 'path', linkValue: '/pages/mine/index', visible: true }
  ]
}

export const DEFAULT_NAVBAR: NavbarConfig = {
  title: '易商城',
  style: 'normal',
  bgColor: '#ffffff',
  textColor: '#333333',
  showBack: true,
  backBehavior: 'back'
}

export const DEFAULT_FLOAT: FloatConfig = {
  enabled: true,
  position: 'right',
  itemList: [
    { key: 'service', name: '客服', icon: '', value: '', enabled: true },
    { key: 'cart', name: '购物车', icon: '', value: '', enabled: true },
    { key: 'top', name: '回到顶部', icon: '', value: '', enabled: true }
  ]
}

/** 默认主题（与后台「活力橙」预设一致）：主题接口异常或未启用主题时兜底 */
export const DEFAULT_THEME: ThemeConfig = {
  primaryColor: '#ff6b00',
  pageBgColor: '#f5f6f7',
  priceColor: '#ff4d1f',
  textColor: '#1f1f1f',
  subTextColor: '#999999'
}

/** 十六进制颜色校验：#RGB 或 #RRGGBB */
function isHexColor(value: unknown): value is string {
  return typeof value === 'string' && /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(value)
}

/** 后端下发的线上主题 JSON → 渲染用配置：逐键校验，非法值回退默认 */
export function parseTheme(data: { themeJson?: string } | null | undefined): ThemeConfig {
  if (!data?.themeJson) return { ...DEFAULT_THEME }
  try {
    const raw = JSON.parse(data.themeJson)
    const pick = (key: keyof ThemeConfig) => (isHexColor(raw?.[key]) ? raw[key] : DEFAULT_THEME[key])
    return {
      pageBgColor: pick('pageBgColor'),
      priceColor: pick('priceColor'),
      primaryColor: pick('primaryColor'),
      subTextColor: pick('subTextColor'),
      textColor: pick('textColor')
    }
  } catch {
    return { ...DEFAULT_THEME }
  }
}

/** 未配置图标时的兜底图标名（与后台 config.ts 的 TAB_ICONS 保持一致） */
export const TAB_ICON_NAMES: Record<string, string> = {
  home: 'lucide:house',
  category: 'lucide:layout-grid',
  cart: 'lucide:shopping-cart',
  mine: 'lucide:user'
}

export const FLOAT_ICON_NAMES: Record<string, string> = {
  service: 'lucide:headphones',
  cart: 'lucide:shopping-cart',
  top: 'lucide:arrow-up'
}

/** 图标仍无法解析时的最终兜底字形 */
export const TAB_GLYPHS: Record<string, string> = {
  home: '🏠',
  category: '▦',
  cart: '🛒',
  mine: '👤'
}

export const FLOAT_GLYPHS: Record<string, string> = {
  service: '💬',
  cart: '🛒',
  top: '↑'
}

/** admin 用 px，小程序统一用 rpx（375 设计稿 1px = 2rpx） */
export function px(value: number | string | undefined, fallback = 0): string {
  const num = Number(value)
  return `${(Number.isFinite(num) ? num : fallback) * 2}rpx`
}

/** 状态栏高度（px，H5 为 0） */
export function statusBarHeight(): number {
  try {
    return uni.getSystemInfoSync().statusBarHeight || 0
  } catch {
    return 0
  }
}

function merge<T>(json: string | null | undefined, fallback: T): T {
  if (!json) return fallback
  try {
    return { ...fallback, ...JSON.parse(json) } as T
  } catch {
    return fallback
  }
}

/** 后端下发的三段 JSON → 渲染用配置 */
export function parseNav(nav: { tabbarJson?: string; navbarJson?: string; floatJson?: string } | null | undefined): NavConfig {
  const tabbar = merge<TabbarConfig>(nav?.tabbarJson, { ...DEFAULT_TABBAR, items: [...DEFAULT_TABBAR.items] })
  const navbar = merge<NavbarConfig>(nav?.navbarJson, { ...DEFAULT_NAVBAR })
  const floatBall = merge<FloatConfig>(nav?.floatJson, {
    ...DEFAULT_FLOAT,
    itemList: [...DEFAULT_FLOAT.itemList]
  })

  if (!Array.isArray(tabbar.items) || tabbar.items.length === 0) {
    tabbar.items = [...DEFAULT_TABBAR.items]
  }
  if (!Array.isArray(floatBall.itemList)) {
    floatBall.itemList = [...DEFAULT_FLOAT.itemList]
  }
  return { tabbar, navbar, floatBall }
}

/** 页面 schema 兜底：过滤未知类型与隐藏组件 */
export function parseSchema(json: string | null | undefined, title = ''): PageSchema {
  const base: PageSchema = { page: { title, bgColor: '#f5f6f7', bgImage: '', padding: 0 }, components: [] }
  if (!json) return base
  try {
    const raw = JSON.parse(json)
    const components: DecorateComponent[] = Array.isArray(raw?.components)
      ? raw.components
          .filter((item: any) => item && typeof item.type === 'string' && item.visible !== false)
          .map((item: any) => ({
            id: item.id || `${item.type}_${Math.random().toString(36).slice(2, 8)}`,
            type: item.type,
            props: item.props || {}
          }))
      : []
    return { page: { ...base.page, ...(raw?.page || {}) }, components }
  } catch {
    return base
  }
}

export interface TabTarget {
  /** 目标路由（不含 query） */
  route: string
  url: string
  /** 是否为 pages.json 中的 tab 页 */
  tab: boolean
  pageId?: string
}

/** TabBar 项 → 可跳转目标 */
export function resolveTabTarget(item: NavTabItem): TabTarget | null {
  if (item.linkType === 'none') return null
  if (item.linkType === 'home') {
    return { route: '/pages/index/index', url: '/pages/index/index', tab: true }
  }
  if (item.linkType === 'page') {
    if (!item.linkValue) return null
    return {
      route: '/pages/decorate/index',
      url: `/pages/decorate/index?id=${item.linkValue}`,
      tab: false,
      pageId: String(item.linkValue)
    }
  }
  const path = item.linkValue?.trim()
  if (!path) return null
  const route = path.split('?')[0]
  return { route, url: path, tab: TAB_ROUTES.includes(route) }
}

/** 判断某个 TabBar 项是否命中当前页面 */
export function isTabActive(item: NavTabItem, currentRoute: string, currentPageId?: string): boolean {
  const target = resolveTabTarget(item)
  if (!target) return false
  if (target.route !== currentRoute) return false
  if (target.pageId) return String(target.pageId) === String(currentPageId ?? '')
  return true
}

function switchTab(url: string) {
  uni.switchTab({
    url,
    fail: () => uni.reLaunch({ url })
  })
}

/** 装修组件里的跳转链接（如 /goods/1、/category/2） */
export function openLink(link?: string): void {
  const path = (link || '').trim()
  if (!path) return
  if (path.startsWith('/goods/')) {
    uni.navigateTo({ url: `/pages/goods/detail?id=${path.split('/')[2]}` })
    return
  }
  if (path.startsWith('/category/')) {
    const categoryId = Number(path.split('/')[2])
    switchTab('/pages/category/index')
    if (Number.isFinite(categoryId)) {
      setTimeout(() => uni.$emit('category:select', categoryId), 120)
    }
    return
  }
  if (path === '/category' || path === '/pages/category/index') {
    switchTab('/pages/category/index')
    return
  }
  if (path === '/cart' || path === '/pages/cart/index') {
    switchTab('/pages/cart/index')
    return
  }
  if (path === '/mine' || path === '/pages/mine/index') {
    switchTab('/pages/mine/index')
    return
  }
  if (path.startsWith('/pages/')) {
    const route = path.split('?')[0]
    if (TAB_ROUTES.includes(route)) {
      switchTab(path)
    } else {
      uni.navigateTo({ url: path })
    }
  }
}

export { switchTab }
