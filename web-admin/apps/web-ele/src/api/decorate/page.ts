import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace DecorateApi {
  export interface Page {
    [key: string]: any;
    id: number;
    isHome: 0 | 1;
    name: string;
    previewCode?: string;
    publishedAt?: string;
    publishedJson?: string;
    remark?: string;
    schemaJson?: string;
    status: 0 | 1;
    type: number;
    updatedAt?: string;
    version?: number;
  }

  export interface PagePayload {
    name: string;
    remark?: string;
    type: number;
  }

  export interface SavePayload {
    name?: string;
    schemaJson: string;
    version?: number;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface PreviewResult {
    id: number;
    name: string;
    previewCode: string;
    schemaJson: string;
    status: number;
  }
}

/** 页面分页列表 */
async function getDecoratePages(params: Recordable<any>) {
  const result = await requestClient.get<
    DecorateApi.PageResult<DecorateApi.Page>
  >('/decorate/pages', { params });
  return { items: result.records, total: result.total };
}

async function getDecoratePage(id: number) {
  return requestClient.get<DecorateApi.Page>(`/decorate/pages/${id}`);
}

async function createDecoratePage(data: DecorateApi.PagePayload) {
  return requestClient.post<DecorateApi.Page>('/decorate/pages', data);
}

async function saveDecoratePage(id: number, data: DecorateApi.SavePayload) {
  return requestClient.put<DecorateApi.Page>(`/decorate/pages/${id}`, data);
}

async function publishDecoratePage(id: number) {
  return requestClient.post<DecorateApi.Page>(`/decorate/pages/${id}/publish`);
}

async function copyDecoratePage(id: number) {
  return requestClient.post<DecorateApi.Page>(`/decorate/pages/${id}/copy`);
}

async function setDecoratePageHome(id: number) {
  return requestClient.put<DecorateApi.Page>(`/decorate/pages/${id}/home`);
}

async function resetDecoratePreviewCode(id: number) {
  return requestClient.put<DecorateApi.Page>(`/decorate/pages/${id}/preview-code`);
}

async function deleteDecoratePage(id: number) {
  return requestClient.delete(`/decorate/pages/${id}`);
}

/** 扫码预览（免登录接口） */
async function getDecoratePreview(code: string) {
  return requestClient.get<DecorateApi.PreviewResult>(
    `/decorate/preview/${code}`,
  );
}

export {
  copyDecoratePage,
  createDecoratePage,
  deleteDecoratePage,
  getDecoratePage,
  getDecoratePages,
  getDecoratePreview,
  publishDecoratePage,
  resetDecoratePreviewCode,
  saveDecoratePage,
  setDecoratePageHome,
};
