import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MarketingCouponApi {
  export interface Coupon {
    [key: string]: any;
    amount?: number;
    /** 对外随机码：小程序端唯一标识，装修组件保存的就是它 */
    couponCode: string;
    createdAt?: string;
    discount?: number;
    id: number;
    name: string;
    perLimit?: number;
    remark?: string;
    sendCount?: number;
    status: 0 | 1;
    threshold?: number;
    totalCount?: number;
    type: number;
    usedCount?: number;
    validDays?: number;
    validEnd?: string;
    validStart?: string;
    validType?: number;
    version?: number;
  }

  export interface CouponPayload {
    amount?: number;
    discount?: number;
    name: string;
    perLimit?: number;
    remark?: string;
    status?: number;
    threshold?: number;
    totalCount: number;
    type: number;
    validDays?: number;
    validEnd?: string;
    validStart?: string;
    validType: number;
    version?: number;
  }

  export interface CouponRecord {
    [key: string]: any;
    couponId: number;
    couponName?: string;
    id: number;
    nickname?: string;
    orderNo?: string;
    receivedAt?: string;
    status: number;
    usedAt?: string;
    userId: number;
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
 * 优惠券分页列表
 */
async function getCouponList(params: Recordable<any>) {
  const result = await requestClient.get<
    MarketingCouponApi.PageResult<MarketingCouponApi.Coupon>
  >('/coupons', { params });
  return { items: result.records, total: result.total };
}

/**
 * 领券记录分页
 */
async function getCouponRecords(params: Recordable<any>) {
  const result = await requestClient.get<
    MarketingCouponApi.PageResult<MarketingCouponApi.CouponRecord>
  >('/coupons/records', { params });
  return { items: result.records, total: result.total };
}

async function createCoupon(data: MarketingCouponApi.CouponPayload) {
  return requestClient.post('/coupons', data);
}

async function updateCoupon(id: number, data: MarketingCouponApi.CouponPayload) {
  return requestClient.put(`/coupons/${id}`, data);
}

async function deleteCoupon(id: number) {
  return requestClient.delete(`/coupons/${id}`);
}

async function changeCouponStatus(id: number, status: number) {
  return requestClient.put(`/coupons/${id}/status`, null, {
    params: { status },
  });
}

export {
  changeCouponStatus,
  createCoupon,
  deleteCoupon,
  getCouponList,
  getCouponRecords,
  updateCoupon,
};
