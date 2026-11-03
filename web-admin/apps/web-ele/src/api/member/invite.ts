import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MemberInviteApi {
  /** 邀请记录 */
  export interface InviteRecord {
    id: number;
    memberId: number;
    memberNickname: string;
    inviterId: number;
    inviterNickname: string;
    /** 0 已注册未下单 1 已完成首单 */
    status: number;
    registerPoints: number;
    orderPoints: number;
    createdAt: string;
    orderedAt?: string;
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
 * 邀请记录分页（状态 / 关键词筛选）
 */
async function getInviteRecords(params: Recordable<any>) {
  const result = await requestClient.get<
    MemberInviteApi.PageResult<MemberInviteApi.InviteRecord>
  >('/members/invites', { params });
  return { items: result.records, total: result.total };
}

export { getInviteRecords };
