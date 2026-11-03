import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/**
 * 邀请状态标签
 */
export function useStatusOptions() {
  return [
    { color: 'warning', label: $t('member.invite.statusRegistered'), value: 0 },
    { color: 'success', label: $t('member.invite.statusOrdered'), value: 1 },
  ];
}

/**
 * 邀请记录查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: $t('member.invite.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('member.invite.keyword'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useStatusOptions(),
      },
      fieldName: 'status',
      label: $t('member.invite.status'),
    },
  ];
}

/**
 * 邀请记录表格列
 */
export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'inviterNickname',
      minWidth: 130,
      title: $t('member.invite.inviter'),
    },
    {
      field: 'memberNickname',
      minWidth: 130,
      title: $t('member.invite.member'),
    },
    {
      cellRender: {
        name: 'CellTag',
        options: useStatusOptions(),
      },
      field: 'status',
      title: $t('member.invite.status'),
      width: 120,
    },
    {
      field: 'registerPoints',
      formatter: ({ row }) => `+${row.registerPoints ?? 0}`,
      title: $t('member.invite.registerPoints'),
      width: 130,
    },
    {
      field: 'orderPoints',
      formatter: ({ row }) => `+${row.orderPoints ?? 0}`,
      title: $t('member.invite.orderPoints'),
      width: 130,
    },
    {
      field: 'createdAt',
      formatter: ({ row }) => formatTime(row.createdAt),
      title: $t('member.invite.createdAt'),
      width: 170,
    },
    {
      field: 'orderedAt',
      formatter: ({ row }) => formatTime(row.orderedAt) || '-',
      title: $t('member.invite.orderedAt'),
      width: 170,
    },
  ];
}
