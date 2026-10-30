import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

/**
 * 积分类型标签配置
 */
export function useTypeOptions() {
  return [
    { color: 'success', label: $t('member.points.typeEarn'), value: 'earn' },
    { color: 'warning', label: $t('member.points.typeConsume'), value: 'consume' },
    { color: 'processing', label: $t('member.points.typeAdjust'), value: 'adjust' },
    { color: 'danger', label: $t('member.points.typeRefund'), value: 'refund' },
  ];
}

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/**
 * 积分流水查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        controlsPosition: 'right',
        min: 1,
        placeholder: $t('member.points.userIdPlaceholder'),
      },
      fieldName: 'userId',
      label: $t('member.points.userId'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useTypeOptions(),
      },
      fieldName: 'type',
      label: $t('member.points.type'),
    },
  ];
}

/**
 * 积分流水表格列
 */
export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'userNickname',
      minWidth: 130,
      title: $t('member.list.nickname'),
    },
    {
      field: 'change',
      formatter: ({ row }) =>
        row.change > 0 ? `+${row.change}` : `${row.change}`,
      title: $t('member.points.change'),
      width: 100,
    },
    {
      field: 'balance',
      title: $t('member.points.balance'),
      width: 100,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: useTypeOptions(),
      },
      field: 'type',
      title: $t('member.points.type'),
      width: 110,
    },
    {
      field: 'orderNo',
      title: $t('member.points.orderNo'),
      width: 180,
    },
    {
      field: 'remark',
      minWidth: 200,
      title: $t('member.points.remark'),
    },
    {
      field: 'createdAt',
      formatter: ({ row }) => formatTime(row.createdAt),
      title: $t('member.points.time'),
      width: 170,
    },
  ];
}
