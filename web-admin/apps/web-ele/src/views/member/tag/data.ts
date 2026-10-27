import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MemberTagApi } from '#/api/member/tag';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/**
 * 标签编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('member.tag.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('member.tag.name'), 2]))
        .max(50, $t('ui.formRules.maxLength', [$t('member.tag.name'), 50])),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('member.tag.colorRed'), value: 'red' },
          { label: $t('member.tag.colorOrange'), value: 'orange' },
          { label: $t('member.tag.colorGreen'), value: 'green' },
          { label: $t('member.tag.colorBlue'), value: 'blue' },
          { label: $t('member.tag.colorDefault'), value: 'default' },
        ],
      },
      fieldName: 'color',
      label: $t('member.tag.color'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: $t('member.tag.remark'),
    },
  ];
}

/**
 * 标签查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('member.tag.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('member.list.keyword'),
    },
  ];
}

/**
 * 标签表格列
 */
export function useColumns<T = MemberTagApi.Tag>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  return [
    {
      field: 'name',
      minWidth: 140,
      title: $t('member.tag.name'),
    },
    {
      field: 'memberCount',
      title: $t('member.tag.memberCount'),
      width: 110,
    },
    {
      field: 'remark',
      minWidth: 240,
      title: $t('member.tag.remark'),
    },
    {
      field: 'createdAt',
      formatter: ({ row }) =>
        row.createdAt
          ? String(row.createdAt).replace('T', ' ').slice(0, 19)
          : '',
      title: $t('member.tag.createdAt'),
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('member.tag.entity'),
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
