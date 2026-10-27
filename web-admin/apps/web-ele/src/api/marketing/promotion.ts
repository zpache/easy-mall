import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MarketingPromotionApi {
  export interface Promotion {
    [key: string]: any;
    createdAt?: string;
    endTime: string;
    goodsId: number;
    goodsName: string;
    groupHours?: number;
    groupSize?: number;
    id: number;
    name: string;
    originalPrice: number;
    promoPrice: number;
    soldCount?: number;
    startTime: string;
    status: 0 | 1;
    stock: number;
    type: number;
    version?: number;
  }

  export interface PromotionPayload {
    endTime: string;
    goodsId: number;
    goodsName: string;
    groupHours?: number;
    groupSize?: number;
    name: string;
    originalPrice: number;
    promoPrice: number;
    startTime: string;
    status?: number;
    stock: number;
    type: number;
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
 * 拼团秒杀活动分页列表
 */
async function getPromotionList(params: Recordable<any>) {
  const result = await requestClient.get<
    MarketingPromotionApi.PageResult<MarketingPromotionApi.Promotion>
  >('/promotions', { params });
  return { items: result.records, total: result.total };
}

async function createPromotion(
  data: MarketingPromotionApi.PromotionPayload,
) {
  return requestClient.post('/promotions', data);
}

async function updatePromotion(
  id: number,
  data: MarketingPromotionApi.PromotionPayload,
) {
  return requestClient.put(`/promotions/${id}`, data);
}

async function deletePromotion(id: number) {
  return requestClient.delete(`/promotions/${id}`);
}

async function changePromotionStatus(id: number, status: number) {
  return requestClient.put(`/promotions/${id}/status`, null, {
    params: { status },
  });
}

export {
  changePromotionStatus,
  createPromotion,
  deletePromotion,
  getPromotionList,
  updatePromotion,
};
