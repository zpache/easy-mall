import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OrderRefundApi } from '#/api/order/refund';

import { $t } from '#/locales';

import { formatMoney, formatTime } from '../list/data';

export function useStatusOptions() {
  return [
    { color: 'warning', label: $t('order.refund.statusPending'), value: 0 },
    { color: 'processing', label: $t('order.refund.statusApproved'), value: 1 },
    { color: 'error', label: $t('order.refund.statusRejected'), value: 2 },
    { color: 'success', label: $t('order.refund.statusRefunded'), value: 3 },
  ];
}

export function useTypeOptions() {
  return [
    { label: $t('order.refund.typeRefund'), value: 'refund' },
    { label: $t('order.refund.typeReturn'), value: 'return' },
  ];
}

export function typeLabel(type?: string) {
  return (
    useTypeOptions().find((item) => item.value === type)?.label ?? type ?? '-'
  );
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('order.refund.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('order.refund.keyword'),
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: useStatusOptions() },
      fieldName: 'status',
      label: $t('order.refund.status'),
    },
    {
      component: 'Select',
      componentProps: { allowClear: true, options: useTypeOptions() },
      fieldName: 'type',
      label: $t('order.refund.type'),
    },
  ];
}

export function useColumns<T = OrderRefundApi.Refund>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  return [
    {
      field: 'refundNo',
      minWidth: 180,
      title: $t('order.refund.refundNo'),
    },
    {
      field: 'wxRefundNo',
      formatter: ({ row }) => row.wxRefundNo || '-',
      minWidth: 200,
      title: $t('order.refund.wxRefundNo'),
    },
    {
      field: 'orderNo',
      minWidth: 180,
      title: $t('order.refund.orderNo'),
    },
    {
      field: 'userNickname',
      formatter: ({ row }) => row.userNickname || $t('order.order.buyerEmpty'),
      title: $t('order.refund.buyer'),
      width: 130,
    },
    {
      field: 'amount',
      formatter: ({ row }) => formatMoney(row.amount),
      title: $t('order.refund.amount'),
      width: 120,
    },
    {
      field: 'type',
      formatter: ({ row }) => typeLabel(row.type),
      title: $t('order.refund.type'),
      width: 110,
    },
    {
      field: 'reason',
      minWidth: 220,
      showOverflow: 'tooltip',
      title: $t('order.refund.reason'),
    },
    {
      cellRender: { name: 'CellTag', options: useStatusOptions() },
      field: 'status',
      title: $t('order.refund.status'),
      width: 110,
    },
    {
      field: 'appliedAt',
      formatter: ({ row }) => formatTime(row.appliedAt),
      title: $t('order.refund.appliedAt'),
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'refundNo',
          nameTitle: $t('order.refund.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'audit',
            show: (row: OrderRefundApi.Refund) => row.status === 0,
            text: $t('order.refund.audit'),
          },
          {
            code: 'confirm',
            show: (row: OrderRefundApi.Refund) => row.status === 1,
            text: $t('order.refund.confirm'),
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('order.refund.operation'),
      width: 150,
    },
  ];
}
