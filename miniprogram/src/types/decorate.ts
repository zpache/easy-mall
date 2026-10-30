/** 装修配置：与 admin 端 widgets/index.ts 的组件口径保持一致 */

export interface NavTabItem {
  key: string
  name: string
  /** 图标：lucide 图标名或图片地址，可为空（走内置兜底）；选中/未选中共用，按状态着色 */
  icon: string
  /** 历史遗留字段：旧数据存过选中态图标，新配置不再区分 */
  selectedIcon?: string
  linkType: 'home' | 'none' | 'page' | 'path'
  /** page 时为装修页面 ID，path 时为以 / 开头的路径 */
  linkValue: string
  visible: boolean
}

export interface TabbarConfig {
  enabled: boolean
  bgColor: string
  color: string
  selectedColor: string
  borderStyle: 'black' | 'none' | 'white'
  items: NavTabItem[]
}

export interface NavbarConfig {
  title: string
  style: 'immersive' | 'normal'
  bgColor: string
  textColor: string
  showBack: boolean
  backBehavior: 'back' | 'home' | 'none'
}

export interface FloatItem {
  key: string
  name: string
  icon: string
  /** 客服项为联系电话 */
  value: string
  enabled: boolean
}

export interface FloatConfig {
  enabled: boolean
  position: 'left' | 'right'
  itemList: FloatItem[]
}

export interface NavConfig {
  tabbar: TabbarConfig
  navbar: NavbarConfig
  floatBall: FloatConfig
}

export interface PageConfig {
  title?: string
  bgColor?: string
  bgImage?: string
  padding?: number
}

/** 线上主题配置（换肤）：与后台「主题换肤」的固定键位一致 */
export interface ThemeConfig {
  /** 主色：按钮、强调、选中态 */
  primaryColor: string
  /** 页面背景色 */
  pageBgColor: string
  /** 价格强调色 */
  priceColor: string
  /** 标题与主要文字色 */
  textColor: string
  /** 次要文字色 */
  subTextColor: string
}

export interface DecorateComponent {
  id: string
  type: string
  visible?: boolean
  props: Record<string, any>
}

export interface PageSchema {
  page: PageConfig
  components: DecorateComponent[]
}

/** /decorate/home、/decorate/page/{id} 返回结构 */
export interface DecoratePageData {
  id: number
  name: string
  type: number
  schemaJson: string
  nav: {
    tabbarJson?: string
    navbarJson?: string
    floatJson?: string
  }
}
