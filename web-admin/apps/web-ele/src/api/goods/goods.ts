import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace GoodsApi {
  export interface Goods {
    [key: string]: any;
    /** 基础销量（运营维护的展示基数） */
    baseSales?: number;
    brandId?: number;
    brandName?: string;
    categoryId: number;
    categoryName?: string;
    createdAt?: string;
    detail?: string;
    id: number;
    images?: string;
    mainImage: string;
    name: string;
    originalPrice?: number;
    price: number;
    sales?: number;
    sort?: number;
    status: 0 | 1;
    stock: number;
    stockWarn?: number;
    subtitle?: string;
    unit?: string;
    updatedAt?: string;
    version?: number;
  }

  export interface GoodsPayload {
    baseSales?: number;
    brandId?: number;
    categoryId: number;
    detail?: string;
    images?: string;
    mainImage: string;
    name: string;
    originalPrice?: number;
    price: number;
    sort?: number;
    status?: number;
    stock: number;
    stockWarn?: number;
    subtitle?: string;
    unit?: string;
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
 * 商品分页列表
 */
async function getGoodsList(params: Recordable<any>) {
  const result = await requestClient.get<GoodsApi.PageResult<GoodsApi.Goods>>(
    '/goods',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getGoodsDetail(id: number) {
  return requestClient.get<GoodsApi.Goods>(`/goods/${id}`);
}

async function createGoods(data: GoodsApi.GoodsPayload) {
  return requestClient.post('/goods', data);
}

async function updateGoods(id: number, data: GoodsApi.GoodsPayload) {
  return requestClient.put(`/goods/${id}`, data);
}

async function deleteGoods(id: number) {
  return requestClient.delete(`/goods/${id}`);
}

async function changeGoodsStatus(id: number, status: number) {
  return requestClient.put(`/goods/${id}/status`, null, {
    params: { status },
  });
}

export {
  changeGoodsStatus,
  createGoods,
  deleteGoods,
  getGoodsDetail,
  getGoodsList,
  updateGoods,
};
