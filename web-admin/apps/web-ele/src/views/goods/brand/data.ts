import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { GoodsBrandApi } from '#/api/goods/brand';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/**
 * 品牌编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('goods.brand.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('goods.brand.name'), 2]))
        .max(50, $t('ui.formRules.maxLength', [$t('goods.brand.name'), 50])),
    },
    {
      component: 'Input',
      componentProps: { placeholder: 'https://' },
      fieldName: 'logo',
      help: $t('goods.brand.logoTip'),
      label: $t('goods.brand.logo'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: $t('goods.brand.sort'),
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
      label: $t('goods.brand.status'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: $t('goods.brand.remark'),
    },
  ];
}

/**
 * 品牌查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: $t('goods.brand.searchPlaceholder') },
      fieldName: 'keyword',
      label: $t('goods.brand.keyword'),
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
      label: $t('goods.brand.status'),
    },
  ];
}

/**
 * 品牌表格列
 */
export function useColumns<T = GoodsBrandApi.Brand>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      cellRender: {
        name: 'CellImage',
        props: { fit: 'cover', style: 'width:40px;height:40px' },
      },
      field: 'logo',
      title: $t('goods.brand.logo'),
      width: 90,
    },
    {
      field: 'name',
      minWidth: 160,
      title: $t('goods.brand.name'),
    },
    {
      field: 'sort',
      title: $t('goods.brand.sort'),
      width: 90,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('goods.brand.status'),
      width: 100,
    },
    {
      field: 'remark',
      minWidth: 180,
      title: $t('goods.brand.remark'),
    },
    {
      field: 'createdAt',
      formatter: ({ row }) =>
        row.createdAt
          ? String(row.createdAt).replace('T', ' ').slice(0, 19)
          : '',
      title: $t('goods.brand.createTime'),
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('goods.brand.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('goods.brand.operation'),
      width: 150,
    },
  ];
}
