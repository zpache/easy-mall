import { requestClient } from '#/api/request';

export namespace SettingsFreightApi {
  export interface FreightConfig {
    defaultFee: number;
    freeAmount: null | number;
    remark?: string;
    version?: number;
  }

  export interface FreightDetail {
    defaultFee: number;
    freeAmount: null | number;
    rules: FreightRule[];
    version?: number;
  }

  export interface FreightRule {
    deliverable: 0 | 1;
    fee: number;
    freeAmount: null | number;
    id: number;
    regionCode: string;
    regionLevel: 1 | 2;
    regionName: string;
    regionPath: string;
    updatedAt?: string;
    version?: number;
  }

  export interface FreightRulePayload {
    deliverable: 0 | 1;
    fee: number;
    freeAmount?: null | number;
    regionCode: string;
    version?: number;
  }

  export interface RegionNode {
    children: RegionNode[];
    code: string;
    level: 1 | 2;
    name: string;
  }
}

/**
 * 运费管理详情（全局配置 + 区域规则）
 */
async function getFreightDetail() {
  return requestClient.get<SettingsFreightApi.FreightDetail>('/freight');
}

/**
 * 更新全局运费配置
 */
async function updateFreightConfig(data: {
  defaultFee: number;
  freeAmount?: null | number;
  version?: number;
}) {
  return requestClient.put('/freight/config', data);
}

/**
 * 省 / 市两级区域树
 */
async function getRegionTree() {
  return requestClient.get<SettingsFreightApi.RegionNode[]>('/freight/regions');
}

/**
 * 新增区域运费规则
 */
async function createFreightRule(data: SettingsFreightApi.FreightRulePayload) {
  return requestClient.post('/freight/rules', data);
}

/**
 * 编辑区域运费规则
 */
async function updateFreightRule(
  id: number,
  data: SettingsFreightApi.FreightRulePayload,
) {
  return requestClient.put(`/freight/rules/${id}`, data);
}

/**
 * 删除区域运费规则
 */
async function deleteFreightRule(id: number) {
  return requestClient.delete(`/freight/rules/${id}`);
}

export {
  createFreightRule,
  deleteFreightRule,
  getFreightDetail,
  getRegionTree,
  updateFreightConfig,
  updateFreightRule,
};
