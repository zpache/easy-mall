import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MarketingReductionApi {
  export interface Reduction {
    [key: string]: any;
    benefit: number;
    createdAt?: string;
    endTime: string;
    id: number;
    mode: number;
    name: string;
    startTime: string;
    status: 0 | 1;
    threshold: number;
    version?: number;
  }

  export interface ReductionPayload {
    benefit: number;
    endTime: string;
    mode: number;
    name: string;
    startTime: string;
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
 * 满减活动分页列表
 */
async function getReductionList(params: Recordable<any>) {
  const result = await requestClient.get<
    MarketingReductionApi.PageResult<MarketingReductionApi.Reduction>
  >('/reductions', { params });
  return { items: result.records, total: result.total };
}

async function createReduction(
  data: MarketingReductionApi.ReductionPayload,
) {
  return requestClient.post('/reductions', data);
}

async function updateReduction(
  id: number,
  data: MarketingReductionApi.ReductionPayload,
) {
  return requestClient.put(`/reductions/${id}`, data);
}

async function deleteReduction(id: number) {
  return requestClient.delete(`/reductions/${id}`);
}

async function changeReductionStatus(id: number, status: number) {
  return requestClient.put(`/reductions/${id}/status`, null, {
    params: { status },
  });
}

export {
  changeReductionStatus,
  createReduction,
  deleteReduction,
  getReductionList,
  updateReduction,
};
