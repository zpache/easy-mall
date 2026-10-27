import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MemberLevelApi } from '#/api/member/level';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/**
 * 等级编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('member.level.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('member.level.name'), 2]))
        .max(50, $t('ui.formRules.maxLength', [$t('member.level.name'), 50])),
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        max: 100,
        min: 50,
        precision: 1,
      },
      defaultValue: 100,
      fieldName: 'discount',
      help: $t('member.level.discountTip'),
      label: $t('member.level.discount'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'threshold',
      help: $t('member.level.thresholdTip'),
      label: $t('member.level.threshold'),
      rules: 'required',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 4, showCount: true },
      fieldName: 'description',
      label: $t('member.level.description'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: $t('member.level.sort'),
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
      label: $t('member.level.status'),
    },
  ];
}

/**
 * 等级查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('member.level.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('member.list.keyword'),
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
      label: $t('member.level.status'),
    },
  ];
}

/**
 * 等级表格列
 */
export function useColumns<T = MemberLevelApi.Level>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      field: 'name',
      minWidth: 140,
      title: $t('member.level.name'),
    },
    {
      field: 'discount',
      formatter: ({ row }) =>
        row.discount === 100 ? $t('member.level.noDiscount') : `${row.discount}%`,
      title: $t('member.level.discount'),
      width: 100,
    },
    {
      field: 'threshold',
      formatter: ({ row }) => `¥${row.threshold}`,
      title: $t('member.level.threshold'),
      width: 120,
    },
    {
      field: 'description',
      minWidth: 240,
      title: $t('member.level.description'),
    },
    {
      field: 'sort',
      title: $t('member.level.sort'),
      width: 80,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('member.level.status'),
      width: 100,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('member.level.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('member.list.operation'),
      width: 150,
    },
  ];
}
