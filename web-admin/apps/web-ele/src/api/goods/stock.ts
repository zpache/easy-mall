import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace GoodsStockApi {
  export interface Stock {
    [key: string]: any;
    brandName?: string;
    categoryName?: string;
    goodsId: number;
    lowStock: boolean;
    mainImage?: string;
    name: string;
    status: 0 | 1;
    stock: number;
    stockWarn: number;
    unit?: string;
    updatedAt?: string;
    version?: number;
  }

  export interface StockLog {
    afterStock: number;
    beforeStock: number;
    changeQty: number;
    createdAt: string;
    createdBy?: number;
    goodsId: number;
    goodsName?: string;
    id: number;
    remark?: string;
    type: 'check' | 'in' | 'out';
  }

  export interface AdjustPayload {
    goodsId: number;
    qty: number;
    remark?: string;
    type: 'check' | 'in' | 'out';
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
 * 库存分页列表
 */
async function getStockList(params: Recordable<any>) {
  const result = await requestClient.get<
    GoodsStockApi.PageResult<GoodsStockApi.Stock>
  >('/stock', { params });
  return { items: result.records, total: result.total };
}

/**
 * 库存调整：入库 / 出库 / 盘点
 */
async function adjustStock(data: GoodsStockApi.AdjustPayload) {
  return requestClient.post('/stock/adjust', data);
}

/**
 * 库存流水分页
 */
async function getStockLogs(params: Recordable<any>) {
  const result = await requestClient.get<
    GoodsStockApi.PageResult<GoodsStockApi.StockLog>
  >('/stock/logs', { params });
  return { items: result.records, total: result.total };
}

export { adjustStock, getStockList, getStockLogs };
