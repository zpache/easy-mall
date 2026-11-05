import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/** 消息类型 → 标签颜色与文案 */
export function useTypeOptions() {
  return [
    {
      color: 'primary',
      label: $t('message.typeOrderPaid'),
      value: 'ORDER_PAID',
    },
    {
      color: 'warning',
      label: $t('message.typeRefundApply'),
      value: 'REFUND_APPLY',
    },
    { color: 'success', label: $t('message.typeComment'), value: 'COMMENT' },
    {
      color: 'error',
      label: $t('message.typeStockWarn'),
      value: 'STOCK_WARN',
    },
    { color: 'default', label: $t('message.typeSystem'), value: 'SYSTEM' },
  ];
}

/** 已读状态选项 */
export function useReadOptions() {
  return [
    { color: 'warning', label: $t('message.unread'), value: 0 },
    { color: 'success', label: $t('message.read'), value: 1 },
  ];
}

/**
 * 消息中心查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useTypeOptions(),
      },
      fieldName: 'type',
      label: $t('message.type'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('message.unread'), value: true },
          { label: $t('message.all'), value: false },
        ],
      },
      fieldName: 'onlyUnread',
      label: $t('message.status'),
    },
  ];
}

/**
 * 消息列表列
 */
export function useColumns(): VxeTableGridColumns {
  return [
    {
      cellRender: {
        name: 'CellTag',
        options: useTypeOptions(),
      },
      field: 'type',
      title: $t('message.type'),
      width: 120,
    },
    {
      field: 'title',
      minWidth: 200,
      title: $t('message.title'),
    },
    {
      field: 'content',
      minWidth: 300,
      showOverflow: 'tooltip',
      title: $t('message.content'),
    },
    {
      field: 'read',
      formatter: ({ row }) =>
        row.read ? $t('message.read') : $t('message.unread'),
      title: $t('message.status'),
      width: 100,
    },
    {
      field: 'createdAt',
      formatter: ({ row }) => formatTime(row.createdAt),
      title: $t('message.createdAt'),
      width: 170,
    },
  ];
}
