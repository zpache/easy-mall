import { requestClient } from '#/api/request';

export namespace DecorateNavApi {
  /** 底部 TabBar 单个标签 */
  export interface TabItem {
    key: string;
    /** 未选中图标（图片地址） */
    icon: string;
    /** 跳转类型：home 商城首页 / page 自定义页面 / path 自定义路径 / none 不跳转 */
    linkType: 'home' | 'none' | 'page' | 'path';
    /** page 时为页面 ID，path 时为以 / 开头的路径 */
    linkValue: string;
    name: string;
    /** 选中态图标（图片地址） */
    selectedIcon: string;
    visible: boolean;
  }

  export interface Tabbar {
    bgColor: string;
    borderStyle: 'black' | 'none' | 'white';
    color: string;
    enabled: boolean;
    items: TabItem[];
    selectedColor: string;
  }

  /** 顶部导航 */
  export interface Navbar {
    /** 返回按钮行为：back 返回上一页 / home 回到首页 / none 不显示返回 */
    backBehavior: 'back' | 'home' | 'none';
    bgColor: string;
    showBack: boolean;
    /** normal 常规 / immersive 沉浸式透明 */
    style: 'immersive' | 'normal';
    textColor: string;
    title: string;
  }

  export interface FloatItem {
    enabled: boolean;
    icon: string;
    /** service 客服 / cart 购物车 / top 回到顶部 */
    key: string;
    name: string;
    /** 客服时为联系电话 */
    value: string;
  }

  /** 快捷入口 / 悬浮球 */
  export interface FloatBall {
    enabled: boolean;
    itemList: FloatItem[];
    position: 'left' | 'right';
  }

  /** 后端存储形态：三段 JSON + 乐观锁版本 */
  export interface Config {
    floatJson: string;
    navbarJson: string;
    tabbarJson: string;
    updatedAt?: string;
    version?: number;
  }

  export interface SavePayload {
    floatJson: string;
    navbarJson: string;
    tabbarJson: string;
    version?: number;
  }
}

function getDecorateNav() {
  return requestClient.get<DecorateNavApi.Config>('/decorate/nav');
}

/** 扫码预览页免登录读取导航配置（只含三段渲染 JSON） */
function getPreviewDecorateNav() {
  return requestClient.get<
    Pick<DecorateNavApi.Config, 'floatJson' | 'navbarJson' | 'tabbarJson'>
  >('/decorate/preview/nav');
}

function saveDecorateNav(data: DecorateNavApi.SavePayload) {
  return requestClient.put<DecorateNavApi.Config>('/decorate/nav', data);
}

/** 发布：把当前草稿快照生效到小程序 */
function publishDecorateNav() {
  return requestClient.post<DecorateNavApi.Config>('/decorate/nav/publish');
}

function resetDecorateNav() {
  return requestClient.post<DecorateNavApi.Config>('/decorate/nav/reset');
}

export {
  getDecorateNav,
  getPreviewDecorateNav,
  publishDecorateNav,
  resetDecorateNav,
  saveDecorateNav,
};
