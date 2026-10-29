import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MemberApi {
  /** 会员标签项 */
  export interface TagItem {
    color?: string;
    id: number;
    name: string;
  }

  export interface Member {
    [key: string]: any;
    avatar?: string;
    createdAt?: string;
    discount?: number;
    id: number;
    levelId: number;
    levelName?: string;
    nickname: string;
    openid?: string;
    orderCount?: number;
    phone?: string;
    points: number;
    source: number;
    tags: TagItem[];
    totalSpend?: number;
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
 * 会员分页列表（关键词 / 等级 / 来源渠道 / 标签筛选）
 */
async function getMemberList(params: Recordable<any>) {
  const result = await requestClient.get<
    MemberApi.PageResult<MemberApi.Member>
  >('/members', { params });
  return { items: result.records, total: result.total };
}

/**
 * 会员详情
 */
async function getMemberDetail(id: number) {
  return requestClient.get<MemberApi.Member>(`/members/${id}`);
}

/**
 * 会员打标签（全量覆盖）
 */
async function updateMemberTags(id: number, tagIds: number[]) {
  return requestClient.put(`/members/${id}/tags`, { tagIds });
}

/**
 * 会员积分人工调整
 */
async function adjustMemberPoints(
  id: number,
  data: { change: number; remark?: string },
) {
  return requestClient.put(`/members/${id}/points`, data);
}

export {
  adjustMemberPoints,
  getMemberDetail,
  getMemberList,
  updateMemberTags,
};
