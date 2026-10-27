import { requestClient } from '#/api/request';

export namespace GoodsCategoryApi {
  export interface Category {
    [key: string]: any;
    children?: Category[];
    createdAt?: string;
    icon?: string;
    id: number;
    name: string;
    parentId: number;
    sort?: number;
    status: 0 | 1;
    version?: number;
  }

  export interface CategoryPayload {
    icon?: string;
    name: string;
    parentId: number;
    sort?: number;
    status?: number;
    version?: number;
  }
}

/**
 * 分类树（分类管理页与下拉树共用）
 */
async function getCategoryTree() {
  return requestClient.get<GoodsCategoryApi.Category[]>('/categories/tree');
}

/**
 * 分类平铺列表
 */
async function getCategoryList() {
  return requestClient.get<GoodsCategoryApi.Category[]>('/categories');
}

async function createCategory(data: GoodsCategoryApi.CategoryPayload) {
  return requestClient.post('/categories', data);
}

async function updateCategory(
  id: number,
  data: GoodsCategoryApi.CategoryPayload,
) {
  return requestClient.put(`/categories/${id}`, data);
}

async function deleteCategory(id: number) {
  return requestClient.delete(`/categories/${id}`);
}

export {
  createCategory,
  deleteCategory,
  getCategoryList,
  getCategoryTree,
  updateCategory,
};
