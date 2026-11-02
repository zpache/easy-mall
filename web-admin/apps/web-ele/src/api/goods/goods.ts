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
    /** 商品编码：系统生成、唯一、不可修改 */
    goodsCode?: string;
    id: number;
    images?: string;
    mainImage: string;
    name: string;
    originalPrice?: number;
    price: number;
    sales?: number;
    sort?: number;
    /** 规格定义 JSON，为空表示单规格商品 */
    specs?: null | string;
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

  /** 规格组：如 { name: '颜色', values: ['红','蓝'] } */
  export interface SpecGroup {
    name: string;
    values: string[];
  }

  /** SKU 行 */
  export interface Sku {
    id?: number;
    skuCode?: string;
    specText: string;
    price: number;
    originalPrice?: null | number;
    stock: number;
    sales?: number;
    image?: null | string;
    status?: number;
  }

  export interface SkuConfig {
    goodsId: number;
    /** 规格定义 JSON，为空表示单规格 */
    specs: null | string;
    skus: Sku[];
  }

  export interface SkuConfigPayload {
    specs: null | string;
    skus: Array<{
      id?: number;
      image?: null | string;
      originalPrice?: null | number;
      price: number;
      specText: string;
      status?: number;
      stock: number;
    }>;
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

/**
 * 商品的规格定义与 SKU 列表
 */
async function getGoodsSku(goodsId: number) {
  return requestClient.get<GoodsApi.SkuConfig>(`/goods/${goodsId}/sku`);
}

/**
 * 保存规格与 SKU（specs 传 null 表示改回单规格）
 */
async function saveGoodsSku(goodsId: number, data: GoodsApi.SkuConfigPayload) {
  return requestClient.put<GoodsApi.SkuConfig>(
    `/goods/${goodsId}/sku`,
    data,
  );
}

export {
  changeGoodsStatus,
  createGoods,
  deleteGoods,
  getGoodsDetail,
  getGoodsList,
  getGoodsSku,
  saveGoodsSku,
  updateGoods,
};
