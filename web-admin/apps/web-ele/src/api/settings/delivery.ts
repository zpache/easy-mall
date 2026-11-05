import { requestClient } from '#/api/request';

export namespace SettingsDeliveryApi {
  export interface DeliveryCompany {
    id: number;
    /** 展示名，发货时写入订单 */
    name: string;
    /** 快递100 公司编码，如 shunfeng */
    code: string;
    /** 查询轨迹是否必须传收件人手机号后四位 */
    phoneRequired: boolean;
    sort: number;
    /** 1 启用 0 停用 */
    status: number;
    remark?: null | string;
    updatedAt?: string;
  }

  export interface DeliveryCompanyPayload {
    name: string;
    code: string;
    phoneRequired: boolean;
    sort: number;
    status: number;
    remark?: null | string;
  }
}

/**
 * 快递公司列表（status=1 只取启用的，发货弹窗下拉用）
 */
async function getDeliveryCompanies(params?: { status?: number }) {
  return requestClient.get<SettingsDeliveryApi.DeliveryCompany[]>(
    '/delivery-companies',
    { params },
  );
}

/**
 * 新增快递公司
 */
async function createDeliveryCompany(
  data: SettingsDeliveryApi.DeliveryCompanyPayload,
) {
  return requestClient.post<number>('/delivery-companies', data);
}

/**
 * 编辑快递公司
 */
async function updateDeliveryCompany(
  id: number,
  data: SettingsDeliveryApi.DeliveryCompanyPayload,
) {
  return requestClient.put(`/delivery-companies/${id}`, data);
}

/**
 * 删除快递公司
 */
async function deleteDeliveryCompany(id: number) {
  return requestClient.delete(`/delivery-companies/${id}`);
}

export {
  createDeliveryCompany,
  deleteDeliveryCompany,
  getDeliveryCompanies,
  updateDeliveryCompany,
};
