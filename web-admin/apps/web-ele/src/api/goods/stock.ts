import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace GoodsStockApi {
  export interface Stock {
    [key: string]: any;
    brandName?: string;
    categoryName?: string;
    goodsCode?: string;
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

  export interface BatchStockFailure {
    goodsCode: string;
    goodsName?: string;
    reason: string;
  }

  export interface BatchStockResult {
    failures: BatchStockFailure[];
    successCount: number;
    total: number;
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

/**
 * 批量设置库存模板下载（xlsx Blob，模板已预填全部商品）
 */
async function downloadBatchStockTemplate() {
  return requestClient.download<Blob>('/stock/batch-template');
}

/**
 * 批量设置库存（上传按模板填写的 xlsx）
 */
async function batchSetStock(file: File) {
  return requestClient.upload<GoodsStockApi.BatchStockResult>(
    '/stock/batch-set',
    { file },
  );
}

/** 浏览器端保存 Blob 文件 */
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export {
  adjustStock,
  batchSetStock,
  downloadBatchStockTemplate,
  downloadBlob,
  getStockList,
  getStockLogs,
};
