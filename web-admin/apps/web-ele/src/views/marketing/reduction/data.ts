import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MarketingReductionApi } from '#/api/marketing/reduction';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/** 满减模式标签配置 */
export function useModeOptions() {
  return [
    { color: 'warning', label: $t('marketing.reduction.modeStep'), value: 1 },
    { color: 'danger', label: $t('marketing.reduction.modeEvery'), value: 2 },
  ];
}

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/**
 * 满减活动编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('marketing.reduction.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('marketing.reduction.name'), 2]))
        .max(100, $t('ui.formRules.maxLength', [$t('marketing.reduction.name'), 100])),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: useModeOptions(),
      },
      defaultValue: 1,
      fieldName: 'mode',
      label: $t('marketing.reduction.mode'),
      help: $t('marketing.reduction.modeTip'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0.01, precision: 2 },
      fieldName: 'threshold',
      label: $t('marketing.reduction.threshold'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0.01, precision: 2 },
      fieldName: 'benefit',
      help: $t('marketing.reduction.benefitTip'),
      label: $t('marketing.reduction.benefit'),
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: $t('marketing.reduction.startTime'),
        type: 'datetime',
        valueFormat: 'YYYY-MM-DDTHH:mm:ss',
      },
      fieldName: 'startTime',
      label: $t('marketing.reduction.startTime'),
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: $t('marketing.reduction.endTime'),
        type: 'datetime',
        valueFormat: 'YYYY-MM-DDTHH:mm:ss',
      },
      fieldName: 'endTime',
      label: $t('marketing.reduction.endTime'),
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
      label: $t('marketing.reduction.status'),
    },
  ];
}

/**
 * 满减活动查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('marketing.reduction.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('marketing.reduction.name'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useModeOptions(),
      },
      fieldName: 'mode',
      label: $t('marketing.reduction.mode'),
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
      label: $t('marketing.reduction.status'),
    },
  ];
}

/**
 * 满减活动表格列
 */
export function useColumns<T = MarketingReductionApi.Reduction>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    { field: 'name', minWidth: 200, title: $t('marketing.reduction.name') },
    {
      cellRender: { name: 'CellTag', options: useModeOptions() },
      field: 'mode',
      title: $t('marketing.reduction.mode'),
      width: 110,
    },
    {
      field: 'rule',
      formatter: ({ row }) =>
        row.mode === 2
          ? $t('marketing.reduction.ruleEvery', [row.threshold, row.benefit])
          : $t('marketing.reduction.ruleStep', [row.threshold, row.benefit]),
      minWidth: 160,
      title: $t('marketing.reduction.rule'),
    },
    {
      field: 'timeRange',
      formatter: ({ row }) =>
        `${formatTime(row.startTime).slice(0, 16)} ~ ${formatTime(row.endTime).slice(5, 16)}`,
      minWidth: 240,
      title: $t('marketing.reduction.timeRange'),
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('marketing.reduction.status'),
      width: 90,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('marketing.reduction.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('marketing.reduction.operation'),
      width: 150,
    },
  ];
}
