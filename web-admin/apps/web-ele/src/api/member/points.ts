import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MemberPointsApi {
  export interface PointsLog {
    balance: number;
    change: number;
    createdAt?: string;
    id: number;
    orderNo?: string;
    remark?: string;
    type: string;
    userId: number;
    userNickname?: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  /** 积分规则：订单支付后按「实付金额 × rate」发放 */
  export interface PointsRule {
    enabled: boolean;
    rate: number;
  }
}

/**
 * 积分流水分页
 */
async function getPointsLogs(params: Recordable<any>) {
  const result = await requestClient.get<
    MemberPointsApi.PageResult<MemberPointsApi.PointsLog>
  >('/points', { params });
  return { items: result.records, total: result.total };
}

/**
 * 积分规则
 */
async function getPointsRule() {
  return requestClient.get<MemberPointsApi.PointsRule>('/points/rule');
}

/**
 * 保存积分规则
 */
async function updatePointsRule(data: MemberPointsApi.PointsRule) {
  return requestClient.put('/points/rule', data);
}

export { getPointsLogs, getPointsRule, updatePointsRule };
