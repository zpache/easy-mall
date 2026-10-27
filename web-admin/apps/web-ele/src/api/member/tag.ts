import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MemberTagApi {
  export interface Tag {
    [key: string]: any;
    color?: string;
    createdAt?: string;
    id: number;
    memberCount?: number;
    name: string;
    remark?: string;
    version?: number;
  }

  export interface TagPayload {
    color?: string;
    name: string;
    remark?: string;
    version?: number;
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
 * 标签分页列表（含会员数）
 */
async function getTagList(params: Recordable<any>) {
  const result = await requestClient.get<
    MemberTagApi.PageResult<MemberTagApi.Tag>
  >('/member-tags', { params });
  return { items: result.records, total: result.total };
}

/**
 * 全部标签（打标下拉用）
 */
async function getTagOptions() {
  return requestClient.get<MemberTagApi.Tag[]>('/member-tags/options');
}

async function createTag(data: MemberTagApi.TagPayload) {
  return requestClient.post('/member-tags', data);
}

async function updateTag(id: number, data: MemberTagApi.TagPayload) {
  return requestClient.put(`/member-tags/${id}`, data);
}

async function deleteTag(id: number) {
  return requestClient.delete(`/member-tags/${id}`);
}

export { createTag, deleteTag, getTagList, getTagOptions, updateTag };
