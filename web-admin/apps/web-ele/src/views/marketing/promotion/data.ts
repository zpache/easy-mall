import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MarketingPromotionApi } from '#/api/marketing/promotion';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/** 活动类型标签配置 */
export function usePromotionTypeOptions() {
  return [
    { color: 'danger', label: $t('marketing.promotion.typeSeckill'), value: 1 },
    { color: 'warning', label: $t('marketing.promotion.typeGroupon'), value: 2 },
  ];
}

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/**
 * 拼团秒杀编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('marketing.promotion.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('marketing.promotion.name'), 2]))
        .max(100, $t('ui.formRules.maxLength', [$t('marketing.promotion.name'), 100])),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: usePromotionTypeOptions(),
      },
      defaultValue: 1,
      fieldName: 'type',
      label: $t('marketing.promotion.type'),
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        controlsPosition: 'right',
        min: 1,
        placeholder: $t('marketing.promotion.goodsIdPlaceholder'),
      },
      fieldName: 'goodsId',
      label: $t('marketing.promotion.goodsId'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'goodsName',
      label: $t('marketing.promotion.goodsName'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0.01, precision: 2 },
      fieldName: 'originalPrice',
      label: $t('marketing.promotion.originalPrice'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0.01, precision: 2 },
      fieldName: 'promoPrice',
      help: $t('marketing.promotion.promoPriceTip'),
      label: $t('marketing.promotion.promoPrice'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 2 },
      defaultValue: 3,
      dependencies: {
        if: (values) => values.type === 2,
        triggerFields: ['type'],
      },
      fieldName: 'groupSize',
      label: $t('marketing.promotion.groupSize'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      defaultValue: 24,
      dependencies: {
        if: (values) => values.type === 2,
        triggerFields: ['type'],
      },
      fieldName: 'groupHours',
      label: $t('marketing.promotion.groupHours'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      defaultValue: 100,
      fieldName: 'stock',
      label: $t('marketing.promotion.stock'),
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: $t('marketing.promotion.startTime'),
        type: 'datetime',
        valueFormat: 'YYYY-MM-DDTHH:mm:ss',
      },
      fieldName: 'startTime',
      label: $t('marketing.promotion.startTime'),
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: $t('marketing.promotion.endTime'),
        type: 'datetime',
        valueFormat: 'YYYY-MM-DDTHH:mm:ss',
      },
      fieldName: 'endTime',
      label: $t('marketing.promotion.endTime'),
      rules: 'required',
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
      label: $t('marketing.promotion.status'),
    },
  ];
}

/**
 * 拼团秒杀查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('marketing.promotion.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('marketing.promotion.name'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: usePromotionTypeOptions(),
      },
      fieldName: 'type',
      label: $t('marketing.promotion.type'),
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
      label: $t('marketing.promotion.status'),
    },
  ];
}

/**
 * 拼团秒杀表格列
 */
export function useColumns<T = MarketingPromotionApi.Promotion>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    { field: 'name', minWidth: 170, title: $t('marketing.promotion.name') },
    {
      cellRender: { name: 'CellTag', options: usePromotionTypeOptions() },
      field: 'type',
      title: $t('marketing.promotion.type'),
      width: 90,
    },
    { field: 'goodsName', minWidth: 180, title: $t('marketing.promotion.goodsName') },
    {
      field: 'promoPrice',
      formatter: ({ row }) =>
        `¥${row.originalPrice} → ¥${row.promoPrice}`,
      title: $t('marketing.promotion.price'),
      width: 170,
    },
    {
      field: 'groupInfo',
      formatter: ({ row }) =>
        row.type === 2
          ? $t('marketing.promotion.groupInfo', [row.groupSize, row.groupHours])
          : '-',
      title: $t('marketing.promotion.groupRule'),
      width: 150,
    },
    {
      field: 'soldProgress',
      formatter: ({ row }) => `${row.soldCount ?? 0} / ${row.stock ?? 0}`,
      title: $t('marketing.promotion.soldProgress'),
      width: 100,
    },
    {
      field: 'timeRange',
      formatter: ({ row }) =>
        `${formatTime(row.startTime).slice(0, 16)} ~ ${formatTime(row.endTime).slice(5, 16)}`,
      minWidth: 240,
      title: $t('marketing.promotion.timeRange'),
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('marketing.promotion.status'),
      width: 90,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('marketing.promotion.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('marketing.promotion.operation'),
      width: 150,
    },
  ];
}
