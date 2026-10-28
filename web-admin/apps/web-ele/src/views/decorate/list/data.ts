import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { DecorateApi } from '#/api/decorate/page';

import { z } from '#/adapter/form';

export const PAGE_TYPES = [
  { label: '首页', value: 1 },
  { label: '分类页', value: 2 },
  { label: '微页面', value: 3 },
];

/** 新建页面可选类型：商城首页固定唯一（随种子数据初始化且不可删除），不允许再建 */
export const NEW_PAGE_TYPES = [
  { label: '分类页', value: 2 },
  { label: '微页面', value: 3 },
];

export function useTypeOptions() {
  return [
    { color: 'danger', label: '首页', value: 1 },
    { color: 'warning', label: '分类页', value: 2 },
    { color: 'processing', label: '微页面', value: 3 },
  ];
}

export function useStatusOptions() {
  return [
    { color: 'info', label: '草稿', value: 0 },
    { color: 'success', label: '已发布', value: 1 },
  ];
}

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-';
}

/** 新建页面表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: '页面名称',
      rules: z
        .string()
        .min(2, '页面名称至少 2 个字符')
        .max(100, '页面名称不能超过 100 个字符'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: NEW_PAGE_TYPES,
      },
      defaultValue: 3,
      fieldName: 'type',
      help: '商城首页固定唯一（列表中带 🏠 标识），此处新建分类页或活动微页面',
      label: '页面类型',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

/** 列表查询表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: '页面名称…' },
      fieldName: 'keyword',
      label: '页面名称',
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: PAGE_TYPES },
      fieldName: 'type',
      label: '页面类型',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '草稿', value: 0 },
          { label: '已发布', value: 1 },
        ],
      },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

/** 列表列 */
export function useColumns<T = DecorateApi.Page>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  return [
    {
      field: 'name',
      formatter: ({ row }) => (row.isHome === 1 ? `🏠 ${row.name}` : row.name),
      minWidth: 180,
      title: '页面名称',
    },
    {
      cellRender: { name: 'CellTag', options: useTypeOptions() },
      field: 'type',
      title: '类型',
      width: 100,
    },
    {
      cellRender: { name: 'CellTag', options: useStatusOptions() },
      field: 'status',
      title: '状态',
      width: 100,
    },
    {
      field: 'previewCode',
      title: '预览码',
      width: 130,
    },
    {
      field: 'updatedAt',
      formatter: ({ row }) => formatTime(row.updatedAt),
      title: '最近编辑',
      width: 170,
    },
    {
      field: 'publishedAt',
      formatter: ({ row }) => formatTime(row.publishedAt),
      title: '发布时间',
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: '页面',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'edit', text: '装修' },
          { code: 'preview', text: '预览' },
          { code: 'copy', text: '复制' },
          { code: 'delete', text: '删除' },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      width: 260,
    },
  ];
}
