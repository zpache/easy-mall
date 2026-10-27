import type { RouteRecordStringComponent } from '@vben/types';

import { requestClient } from '#/api/request';

/**
 * 获取当前用户可见的菜单（vben 路由格式），登录后由后端按角色动态下发
 */
export async function getAllMenusApi() {
  return requestClient.get<RouteRecordStringComponent[]>(
    '/system/menu/routes',
  );
}
