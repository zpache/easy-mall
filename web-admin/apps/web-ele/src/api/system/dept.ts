import { requestClient } from '#/api/request';

export namespace SystemDeptApi {
  /** 部门树节点 */
  export interface SystemDept {
    [key: string]: any;
    children?: SystemDept[];
    createdAt?: string;
    id: number;
    name: string;
    pid: number;
    remark?: string;
    sort?: number;
    status: 0 | 1;
  }

  /** 新增 / 编辑提交数据 */
  export interface DeptPayload {
    name: string;
    pid?: number;
    remark?: string;
    sort?: number;
    status?: number;
  }
}

/**
 * 部门树
 */
async function getDeptList() {
  return requestClient.get<Array<SystemDeptApi.SystemDept>>(
    '/system/dept/list',
  );
}

/**
 * 新增部门
 */
async function createDept(data: SystemDeptApi.DeptPayload) {
  return requestClient.post('/system/dept', data);
}

/**
 * 编辑部门
 */
async function updateDept(id: number, data: SystemDeptApi.DeptPayload) {
  return requestClient.put(`/system/dept/${id}`, data);
}

/**
 * 删除部门
 */
async function deleteDept(id: number) {
  return requestClient.delete(`/system/dept/${id}`);
}

export { createDept, deleteDept, getDeptList, updateDept };
