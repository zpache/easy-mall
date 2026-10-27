import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MarketingSigninApi {
  export interface SigninRule {
    [key: string]: any;
    bonusEveryDays?: number;
    bonusPoints?: number;
    createdAt?: string;
    id: number;
    name: string;
    pointsPerDay: number;
    remark?: string;
    status: 0 | 1;
    version?: number;
  }

  export interface SigninRulePayload {
    bonusEveryDays?: number;
    bonusPoints?: number;
    name: string;
    pointsPerDay: number;
    remark?: string;
    status?: number;
    version?: number;
  }

  export interface SigninLog {
    [key: string]: any;
    continuousDays: number;
    createdAt?: string;
    id: number;
    nickname?: string;
    points: number;
    signDate: string;
    userId: number;
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
 * 签到规则分页列表
 */
async function getSigninRuleList(params: Recordable<any>) {
  const result = await requestClient.get<
    MarketingSigninApi.PageResult<MarketingSigninApi.SigninRule>
  >('/signin-rules', { params });
  return { items: result.records, total: result.total };
}

async function createSigninRule(data: MarketingSigninApi.SigninRulePayload) {
  return requestClient.post('/signin-rules', data);
}

async function updateSigninRule(
  id: number,
  data: MarketingSigninApi.SigninRulePayload,
) {
  return requestClient.put(`/signin-rules/${id}`, data);
}

async function deleteSigninRule(id: number) {
  return requestClient.delete(`/signin-rules/${id}`);
}

async function changeSigninRuleStatus(id: number, status: number) {
  return requestClient.put(`/signin-rules/${id}/status`, null, {
    params: { status },
  });
}

/**
 * 签到记录分页
 */
async function getSigninLogs(params: Recordable<any>) {
  const result = await requestClient.get<
    MarketingSigninApi.PageResult<MarketingSigninApi.SigninLog>
  >('/signin-logs', { params });
  return { items: result.records, total: result.total };
}

export {
  changeSigninRuleStatus,
  createSigninRule,
  deleteSigninRule,
  getSigninLogs,
  getSigninRuleList,
  updateSigninRule,
};
