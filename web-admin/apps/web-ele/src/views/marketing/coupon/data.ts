import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MarketingCouponApi } from '#/api/marketing/coupon';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/** 券类型标签配置 */
export function useCouponTypeOptions() {
  return [
    { color: 'danger', label: $t('marketing.coupon.typeFull'), value: 1 },
    { color: 'warning', label: $t('marketing.coupon.typeDiscount'), value: 2 },
    { color: 'success', label: $t('marketing.coupon.typeCash'), value: 3 },
  ];
}

/** 核销状态标签配置 */
export function useRecordStatusOptions() {
  return [
    { color: 'info', label: $t('marketing.coupon.recordUnused'), value: 0 },
    { color: 'success', label: $t('marketing.coupon.recordUsed'), value: 1 },
    { color: 'default', label: $t('marketing.coupon.recordExpired'), value: 2 },
  ];
}

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/**
 * 优惠券编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('marketing.coupon.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('marketing.coupon.name'), 2]))
        .max(100, $t('ui.formRules.maxLength', [$t('marketing.coupon.name'), 100])),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: useCouponTypeOptions(),
      },
      defaultValue: 1,
      fieldName: 'type',
      label: $t('marketing.coupon.type'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0.01, precision: 2 },
      dependencies: {
        if: (values) => values.type !== 2,
        triggerFields: ['type'],
      },
      fieldName: 'amount',
      help: $t('marketing.coupon.amountTip'),
      label: $t('marketing.coupon.amount'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', max: 99.9, min: 50, precision: 1 },
      dependencies: {
        if: (values) => values.type === 2,
        triggerFields: ['type'],
      },
      fieldName: 'discount',
      help: $t('marketing.coupon.discountTip'),
      label: $t('marketing.coupon.discount'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      defaultValue: 0,
      fieldName: 'threshold',
      help: $t('marketing.coupon.thresholdTip'),
      label: $t('marketing.coupon.threshold'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      defaultValue: 100,
      fieldName: 'totalCount',
      label: $t('marketing.coupon.total'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      defaultValue: 1,
      fieldName: 'perLimit',
      label: $t('marketing.coupon.perLimit'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('marketing.coupon.validTypeDate'), value: 1 },
          { label: $t('marketing.coupon.validTypeDays'), value: 2 },
        ],
      },
      defaultValue: 1,
      fieldName: 'validType',
      label: $t('marketing.coupon.validType'),
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        type: 'daterange',
        valueFormat: 'YYYY-MM-DD',
      },
      dependencies: {
        if: (values) => values.validType === 1,
        triggerFields: ['validType'],
      },
      fieldName: 'validRange',
      label: $t('marketing.coupon.validRange'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      defaultValue: 7,
      dependencies: {
        if: (values) => values.validType === 2,
        triggerFields: ['validType'],
      },
      fieldName: 'validDays',
      label: $t('marketing.coupon.validDays'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('common.enabled'), value: 1 },
          { label: $t('common.disabled'), value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: $t('marketing.coupon.status'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: $t('marketing.coupon.remark'),
    },
  ];
}

/**
 * 优惠券查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('marketing.coupon.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('marketing.coupon.name'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useCouponTypeOptions(),
      },
      fieldName: 'type',
      label: $t('marketing.coupon.type'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('common.enabled'), value: 1 },
          { label: $t('common.disabled'), value: 0 },
        ],
      },
      fieldName: 'status',
      label: $t('marketing.coupon.status'),
    },
  ];
}

/**
 * 优惠券表格列
 */
export function useColumns<T = MarketingCouponApi.Coupon>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    { field: 'name', minWidth: 180, title: $t('marketing.coupon.name') },
    {
      cellRender: { name: 'CellTag', options: useCouponTypeOptions() },
      field: 'type',
      title: $t('marketing.coupon.type'),
      width: 100,
    },
    {
      field: 'amount',
      formatter: ({ row }) =>
        row.type === 2 ? `${row.discount}%` : `¥${row.amount}`,
      title: $t('marketing.coupon.face'),
      width: 100,
    },
    {
      field: 'threshold',
      formatter: ({ row }) =>
        Number(row.threshold) === 0 ? $t('marketing.coupon.noThreshold') : `¥${row.threshold}`,
      title: $t('marketing.coupon.threshold'),
      width: 120,
    },
    {
      field: 'sendCount',
      formatter: ({ row }) => `${row.sendCount ?? 0} / ${row.totalCount ?? 0}`,
      title: $t('marketing.coupon.progress'),
      width: 110,
    },
    {
      field: 'usedCount',
      title: $t('marketing.coupon.used'),
      width: 90,
    },
    {
      field: 'validRange',
      formatter: ({ row }) =>
        row.validType === 1
          ? `${row.validStart} ~ ${row.validEnd}`
          : $t('marketing.coupon.validAfterDays', [row.validDays]),
      minWidth: 200,
      title: $t('marketing.coupon.validRange'),
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('marketing.coupon.status'),
      width: 90,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('marketing.coupon.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('marketing.coupon.operation'),
      width: 150,
    },
  ];
}

/**
 * 领券记录表格列
 */
export function useRecordColumns(): VxeTableGridColumns {
  return [
    { field: 'couponName', minWidth: 160, title: $t('marketing.coupon.name') },
    { field: 'nickname', title: $t('marketing.coupon.recordUser'), width: 110 },
    {
      cellRender: { name: 'CellTag', options: useRecordStatusOptions() },
      field: 'status',
      title: $t('marketing.coupon.recordStatus'),
      width: 100,
    },
    { field: 'orderNo', title: $t('marketing.coupon.recordOrderNo'), width: 180 },
    {
      field: 'receivedAt',
      formatter: ({ row }) => formatTime(row.receivedAt),
      title: $t('marketing.coupon.recordReceivedAt'),
      width: 170,
    },
    {
      field: 'usedAt',
      formatter: ({ row }) => formatTime(row.usedAt),
      title: $t('marketing.coupon.recordUsedAt'),
      width: 170,
    },
  ];
}
