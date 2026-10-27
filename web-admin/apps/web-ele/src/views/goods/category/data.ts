import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { GoodsCategoryApi } from '#/api/goods/category';

import { z } from '#/adapter/form';
import { getCategoryTree } from '#/api/goods/category';
import { $t } from '#/locales';

/**
 * 分类编辑表单
 */
export function useSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('goods.category.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('goods.category.name'), 2]))
        .max(32, $t('ui.formRules.maxLength', [$t('goods.category.name'), 32])),
    },
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: getCategoryTree,
        childrenField: 'children',
        class: 'w-full',
        labelField: 'name',
        placeholder: $t('goods.category.topLevel'),
        valueField: 'id',
      },
      fieldName: 'parentId',
      help: $t('goods.category.parentTip'),
      label: $t('goods.category.parent'),
    },
    {
      component: 'Input',
      componentProps: { placeholder: 'https://' },
      fieldName: 'icon',
      label: $t('goods.category.icon'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: $t('goods.category.sort'),
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
      label: $t('goods.category.status'),
    },
  ];
}

/**
 * 分类表格列
 */
export function useColumns(
  onActionClick?: OnActionClickFn<GoodsCategoryApi.Category>,
): VxeTableGridColumns<GoodsCategoryApi.Category> {
  return [
    {
      align: 'left',
      field: 'name',
      fixed: 'left',
      title: $t('goods.category.name'),
      treeNode: true,
      width: 240,
    },
    {
      field: 'icon',
      formatter: ({ row }) => row.icon ?? '',
      title: $t('goods.category.icon'),
      width: 260,
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'status',
      title: $t('goods.category.status'),
      width: 100,
    },
    {
      field: 'sort',
      title: $t('goods.category.sort'),
      width: 90,
    },
    {
      field: 'createdAt',
      formatter: ({ row }) =>
        row.createdAt ? String(row.createdAt).replace('T', ' ').slice(0, 19) : '',
      title: $t('goods.category.createTime'),
      width: 180,
    },
    {
      align: 'right',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('goods.category.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'append', text: $t('goods.category.append') },
          'edit',
          {
            code: 'delete',
            disabled: (row: GoodsCategoryApi.Category) =>
              !!(row.children && row.children.length > 0),
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('goods.category.operation'),
      width: 220,
    },
  ];
}
