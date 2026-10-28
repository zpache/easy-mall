import type { DecorateNavApi as NavApi } from '#/api/decorate/nav';

/** 与后端校验口径保持一致 */
export const MIN_TABS = 3;
export const MAX_TABS = 5;
export const MAX_FLOAT = 5;

/**
 * 图标留空时的内置线性图标兜底。
 * 取值需在小程序端内置图标集（utils/nav-icons.ts）内，保证三端一致。
 */
export const TAB_ICONS: Record<string, string> = {
  cart: 'lucide:shopping-cart',
  category: 'lucide:layout-grid',
  home: 'lucide:house',
  mine: 'lucide:user',
};

export const FLOAT_ICONS: Record<string, string> = {
  cart: 'lucide:shopping-cart',
  service: 'lucide:headphones',
  top: 'lucide:arrow-up',
};

/** 历史数据里图标字段可能是上传的图片地址 */
export function isIconImage(value: string | undefined) {
  return /^(?:https?:)?\/\//.test(value ?? '');
}

export interface NavConfig {
  floatBall: NavApi.FloatBall;
  navbar: NavApi.Navbar;
  tabbar: NavApi.Tabbar;
}

export function tabItem(
  name: string,
  key: string,
  linkType: NavApi.TabItem['linkType'],
  linkValue = '',
): NavApi.TabItem {
  return {
    icon: '',
    key,
    linkType,
    linkValue,
    name,
    selectedIcon: '',
    visible: true,
  };
}

export const DEFAULT_TABBAR: NavApi.Tabbar = {
  bgColor: '#ffffff',
  borderStyle: 'black',
  color: '#909399',
  enabled: true,
  items: [
    tabItem('首页', 'home', 'home'),
    tabItem('分类', 'category', 'path', '/pages/category/index'),
    tabItem('购物车', 'cart', 'path', '/pages/cart/index'),
    tabItem('我的', 'mine', 'path', '/pages/mine/index'),
  ],
  selectedColor: '#1677ff',
};

export const DEFAULT_NAVBAR: NavApi.Navbar = {
  backBehavior: 'back',
  bgColor: '#ffffff',
  showBack: true,
  style: 'normal',
  textColor: '#333333',
  title: '易商城',
};

export const DEFAULT_FLOAT: NavApi.FloatBall = {
  enabled: true,
  itemList: [
    { enabled: true, icon: '', key: 'service', name: '客服', value: '' },
    { enabled: true, icon: '', key: 'cart', name: '购物车', value: '' },
    { enabled: true, icon: '', key: 'top', name: '回到顶部', value: '' },
  ],
  position: 'right',
};

export function createDefaultNav(): NavConfig {
  return {
    floatBall: structuredClone(DEFAULT_FLOAT),
    navbar: { ...DEFAULT_NAVBAR },
    tabbar: structuredClone(DEFAULT_TABBAR),
  };
}

function merge<T>(json: string | undefined, fallback: T): T {
  if (!json) {
    return fallback;
  }
  try {
    return { ...fallback, ...JSON.parse(json) } as T;
  } catch {
    return fallback;
  }
}

/**
 * 后端存的是三段 JSON 字符串，这里统一解析成渲染用的对象。
 * 装修编辑器、导航装修页、扫码预览页共用同一份口径。
 */
export function parseNavConfig(
  config: null | Partial<NavApi.Config> | undefined,
): NavConfig {
  const fallback = createDefaultNav();
  const tabbar = merge<NavApi.Tabbar>(
    config?.tabbarJson,
    structuredClone(fallback.tabbar),
  );
  const navbar = merge<NavApi.Navbar>(config?.navbarJson, {
    ...fallback.navbar,
  });
  const floatBall = merge<NavApi.FloatBall>(config?.floatJson, {
    ...fallback.floatBall,
  });

  if (!Array.isArray(tabbar.items)) {
    tabbar.items = structuredClone(fallback.tabbar.items);
  }
  if (!Array.isArray(floatBall.itemList)) {
    floatBall.itemList = structuredClone(fallback.floatBall.itemList);
  }
  return { floatBall, navbar, tabbar };
}

export function tabbarBorder(type: string | undefined) {
  if (type === 'none') {
    return 'none';
  }
  return `1px solid ${type === 'white' ? '#ffffff' : 'rgb(0 0 0 / 8%)'}`;
}
