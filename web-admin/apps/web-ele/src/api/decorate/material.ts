import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MaterialApi {
  export interface Material {
    createdAt?: string;
    id: number;
    mime?: string;
    name: string;
    objectKey: string;
    scene?: string;
    size: number;
    url: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  export interface QueryParams extends Recordable<any> {
    keyword?: string;
    /** 图片 / 视频 / 音频，按 mime 前缀过滤；空为全部 */
    mediaType?: string;
    pageNum: number;
    pageSize: number;
    scene?: string;
  }
}

function getMaterials(params: MaterialApi.QueryParams) {
  return requestClient.get<MaterialApi.PageResult<MaterialApi.Material>>(
    '/files',
    { params },
  );
}

function renameMaterial(id: number, name: string) {
  return requestClient.put<MaterialApi.Material>(`/files/${id}`, { name });
}

function deleteMaterial(id: number) {
  return requestClient.delete(`/files/${id}`);
}

export { deleteMaterial, getMaterials, renameMaterial };
