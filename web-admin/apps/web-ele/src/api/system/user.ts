import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace SystemUserApi {
  export interface SystemUser {
    [key: string]: any;
    createdAt?: string;
    deptId?: number;
    deptName?: string;
    email?: string;
    id: number;
    lastLoginAt?: string;
    nickname: string;
    phone?: string;
    roleIds?: number[];
    roleNames?: string[];
    status: 0 | 1;
    username: string;
    version?: number;
  }

  export interface UserPayload {
    deptId?: number;
    email?: string;
    nickname: string;
    password?: string;
    phone?: string;
    roleIds?: number[];
    status?: number;
    username: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

/**
 * 用户分页列表
 */
async function getUserList(params: Recordable<any>) {
  const result = await requestClient.get<SystemUserApi.PageResult<any>>(
    '/system/user/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function createUser(data: SystemUserApi.UserPayload) {
  return requestClient.post('/system/user', data);
}

async function updateUser(id: number, data: SystemUserApi.UserPayload) {
  return requestClient.put(`/system/user/${id}`, data);
}

async function deleteUser(id: number) {
  return requestClient.delete(`/system/user/${id}`);
}

async function changeUserStatus(id: number, status: number) {
  return requestClient.put(`/system/user/${id}/status`, null, {
    params: { status },
  });
}

async function resetUserPassword(id: number, password: string) {
  return requestClient.put(`/system/user/${id}/password`, null, {
    params: { password },
  });
}

export {
  changeUserStatus,
  createUser,
  deleteUser,
  getUserList,
  resetUserPassword,
  updateUser,
};
