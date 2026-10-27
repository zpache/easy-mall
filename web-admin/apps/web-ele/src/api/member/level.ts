import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MemberLevelApi {
  export interface Level {
    [key: string]: any;
    createdAt?: string;
    description?: string;
    discount: number;
    id: number;
    name: string;
    sort?: number;
    status: 0 | 1;
    threshold: number;
    version?: number;
  }

  export interface LevelPayload {
    description?: string;
    discount: number;
    name: string;
    sort?: number;
    status?: number;
    threshold: number;
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
 * 等级分页列表
 */
async function getLevelList(params: Recordable<any>) {
  const result = await requestClient.get<
    MemberLevelApi.PageResult<MemberLevelApi.Level>
  >('/member-levels', { params });
  return { items: result.records, total: result.total };
}

/**
 * 启用中的等级（下拉选项）
 */
async function getLevelOptions() {
  return requestClient.get<MemberLevelApi.Level[]>('/member-levels/options');
}

async function createLevel(data: MemberLevelApi.LevelPayload) {
  return requestClient.post('/member-levels', data);
}

async function updateLevel(id: number, data: MemberLevelApi.LevelPayload) {
  return requestClient.put(`/member-levels/${id}`, data);
}

async function deleteLevel(id: number) {
  return requestClient.delete(`/member-levels/${id}`);
}

async function changeLevelStatus(id: number, status: number) {
  return requestClient.put(`/member-levels/${id}/status`, null, {
    params: { status },
  });
}

export {
  changeLevelStatus,
  createLevel,
  deleteLevel,
  getLevelList,
  getLevelOptions,
  updateLevel,
};
