import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace SystemLogApi {
  export interface LoginLog {
    [key: string]: any;
    createdAt: string;
    id: number;
    ip?: string;
    message?: string;
    success: 0 | 1;
    userAgent?: string;
    userId?: number;
    username?: string;
  }

  export interface OperLog {
    [key: string]: any;
    action?: string;
    createdAt: string;
    errorMsg?: string;
    id: number;
    ip?: string;
    method?: string;
    module?: string;
    params?: string;
    resultCode?: number;
    uri?: string;
    userId?: number;
    username?: string;
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
 * 操作日志分页
 */
async function getOperLogPage(params: Recordable<any>) {
  const result = await requestClient.get<
    SystemLogApi.PageResult<SystemLogApi.OperLog>
  >('/oper-logs', { params });
  return { items: result.records, total: result.total };
}

/**
 * 登录日志分页
 */
async function getLoginLogPage(params: Recordable<any>) {
  const result = await requestClient.get<
    SystemLogApi.PageResult<SystemLogApi.LoginLog>
  >('/login-logs', { params });
  return { items: result.records, total: result.total };
}

export { getLoginLogPage, getOperLogPage };
