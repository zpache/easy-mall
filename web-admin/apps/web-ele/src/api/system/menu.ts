import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace SystemMenuApi {
  /** 徽标颜色集合 */
  export const BadgeVariants = [
    'default',
    'destructive',
    'primary',
    'success',
    'warning',
  ] as const;
  /** 徽标类型集合 */
  export const BadgeTypes = ['dot', 'normal'] as const;
  /** 菜单类型集合 */
  export const MenuTypes = [
    'catalog',
    'menu',
    'embedded',
    'link',
    'button',
  ] as const;

  /** 菜单元信息 */
  export interface SystemMenuMeta {
    [key: string]: any;
    affixTab?: boolean;
    badge?: string;
    badgeType?: (typeof BadgeTypes)[number];
    badgeVariants?: (typeof BadgeVariants)[number];
    hideChildrenInMenu?: boolean;
    hideInBreadcrumb?: boolean;
    hideInMenu?: boolean;
    hideInTab?: boolean;
    icon?: string;
    iframeSrc?: string;
    keepAlive?: boolean;
    link?: string;
    order?: number;
    title?: string;
  }

  /** 系统菜单 */
  export interface SystemMenu {
    [key: string]: any;
    /** 后端权限标识 */
    authCode?: string;
    /** 子级 */
    children?: SystemMenu[];
    /** 组件 */
    component?: string;
    /** 菜单ID */
    id: number;
    /** 菜单元数据 */
    meta?: SystemMenuMeta;
    /** 菜单名称（路由名） */
    name: string;
    /** 路由路径 */
    path?: string;
    /** 父级ID */
    pid: number;
    /** 菜单类型 */
    type: (typeof MenuTypes)[number];
  }

  /** 新增 / 编辑提交数据 */
  export interface MenuPayload {
    authCode?: string;
    component?: string;
    meta?: SystemMenuMeta;
    name: string;
    path?: string;
    pid?: number;
    status?: number;
    type: string;
  }
}

/**
 * 菜单树
 */
async function getMenuList() {
  return requestClient.get<Array<SystemMenuApi.SystemMenu>>(
    '/system/menu/list',
  );
}

/**
 * 当前用户可见菜单树
 */
async function getMenuListByUser() {
  return requestClient.get<Array<SystemMenuApi.SystemMenu>>(
    '/system/menu/list-by-user',
  );
}

async function isMenuNameExists(
  name: string,
  id?: SystemMenuApi.SystemMenu['id'],
) {
  return requestClient.get<boolean>('/system/menu/name-exists', {
    params: { id, name },
  });
}

async function isMenuPathExists(
  path: string,
  id?: SystemMenuApi.SystemMenu['id'],
) {
  return requestClient.get<boolean>('/system/menu/path-exists', {
    params: { id, path },
  });
}

/**
 * 新增菜单
 */
async function createMenu(data: SystemMenuApi.MenuPayload) {
  return requestClient.post('/system/menu', data);
}

/**
 * 编辑菜单
 */
async function updateMenu(id: number, data: SystemMenuApi.MenuPayload) {
  return requestClient.put(`/system/menu/${id}`, data);
}

/**
 * 删除菜单
 */
async function deleteMenu(id: number) {
  return requestClient.delete(`/system/menu/${id}`);
}

/**
 * 菜单树转下拉选项（el-tree-select 用）
 */
function toTreeOptions(
  list: SystemMenuApi.SystemMenu[],
): Array<Recordable<any>> {
  return list.map((item) => ({
    children:
      item.children && item.children.length > 0
        ? toTreeOptions(item.children)
        : undefined,
    label: item.meta?.title ?? item.name,
    value: item.id,
  }));
}

export {
  createMenu,
  deleteMenu,
  getMenuList,
  getMenuListByUser,
  isMenuNameExists,
  isMenuPathExists,
  toTreeOptions,
  updateMenu,
};
