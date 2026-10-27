import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace SystemRoleApi {
  export interface SystemRole {
    [key: string]: any;
    code: string;
    createdAt?: string;
    id: number;
    name: string;
    /** 已授权的菜单 ID */
    permissions?: number[];
    remark?: string;
    sort?: number;
    status: 0 | 1;
    version?: number;
  }

  export interface RolePayload {
    code: string;
    name: string;
    permissions?: number[];
    remark?: string;
    sort?: number;
    status?: number;
  }

  /** 分页结果 */
  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

/**
 * 角色分页列表
 */
async function getRoleList(params: Recordable<any>) {
  const result = await requestClient.get<SystemRoleApi.PageResult<any>>(
    '/system/role/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

/**
 * 启用中的角色（下拉）
 */
async function getRoleOptions() {
  return requestClient.get<Array<SystemRoleApi.SystemRole>>(
    '/system/role/options',
  );
}

async function createRole(data: SystemRoleApi.RolePayload) {
  return requestClient.post('/system/role', data);
}

async function updateRole(id: number, data: SystemRoleApi.RolePayload) {
  return requestClient.put(`/system/role/${id}`, data);
}

async function deleteRole(id: number) {
  return requestClient.delete(`/system/role/${id}`);
}

async function changeRoleStatus(id: number, status: number) {
  return requestClient.put(`/system/role/${id}/status`, null, {
    params: { status },
  });
}

export {
  changeRoleStatus,
  createRole,
  deleteRole,
  getRoleList,
  getRoleOptions,
  updateRole,
};
