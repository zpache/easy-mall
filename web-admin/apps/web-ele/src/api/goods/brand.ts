import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace GoodsBrandApi {
  export interface Brand {
    [key: string]: any;
    createdAt?: string;
    id: number;
    logo?: string;
    name: string;
    remark?: string;
    sort?: number;
    status: 0 | 1;
    version?: number;
  }

  export interface BrandPayload {
    logo?: string;
    name: string;
    remark?: string;
    sort?: number;
    status?: number;
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
 * 品牌分页列表
 */
async function getBrandList(params: Recordable<any>) {
  const result = await requestClient.get<
    GoodsBrandApi.PageResult<GoodsBrandApi.Brand>
  >('/brands', { params });
  return { items: result.records, total: result.total };
}

/**
 * 启用中的品牌（下拉选项）
 */
async function getBrandOptions() {
  return requestClient.get<GoodsBrandApi.Brand[]>('/brands/options');
}

async function createBrand(data: GoodsBrandApi.BrandPayload) {
  return requestClient.post('/brands', data);
}

async function updateBrand(id: number, data: GoodsBrandApi.BrandPayload) {
  return requestClient.put(`/brands/${id}`, data);
}

async function deleteBrand(id: number) {
  return requestClient.delete(`/brands/${id}`);
}

async function changeBrandStatus(id: number, status: number) {
  return requestClient.put(`/brands/${id}/status`, null, {
    params: { status },
  });
}

export {
  changeBrandStatus,
  createBrand,
  deleteBrand,
  getBrandList,
  getBrandOptions,
  updateBrand,
};
