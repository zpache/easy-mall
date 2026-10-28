/** 装修配置：与 admin 端 widgets/index.ts 的组件口径保持一致 */

export interface NavTabItem {
  key: string
  name: string
  /** 未选中图标（图片地址，可为空） */
  icon: string
  selectedIcon: string
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
