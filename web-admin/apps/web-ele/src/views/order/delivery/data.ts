import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/order/order';

import { $t } from '#/locales';

import {
  formatMoney,
  formatTime,
  summaryGoods,
  useRefundStatusOptions,
} from '../list/data';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('order.order.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('order.order.keyword'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useRefundStatusOptions(),
      },
      fieldName: 'refundStatus',
      label: $t('order.order.refundStatus'),
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        endPlaceholder: '',
        rangeSeparator: '~',
        startPlaceholder: '',
        type: 'daterange',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'dateRange',
      label: $t('order.order.dateRange'),
    },
  ];
}

/**
 * 待发货列表列：突出收货信息，方便打单发货
 */
export function useColumns<T = OrderApi.Order>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  return [
    {
      field: 'orderNo',
      minWidth: 180,
      title: $t('order.order.orderNo'),
    },
    {
      field: 'goodsName',
      formatter: ({ row }) => summaryGoods(row),
      minWidth: 220,
      title: $t('order.order.goods'),
    },
    {
      field: 'receiverName',
      formatter: ({ row }) =>
        `${row.receiverName} ${row.receiverPhone ?? ''}\n${row.receiverAddress ?? ''}`,
      minWidth: 260,
      title: $t('order.order.receiverInfo'),
    },
    {
      field: 'payAmount',
      formatter: ({ row }) => formatMoney(row.payAmount),
      title: $t('order.order.amount'),
      width: 120,
    },
    {
      field: 'payTime',
      formatter: ({ row }) => formatTime(row.payTime) || '-',
      title: $t('order.order.payTime'),
      width: 170,
    },
    {
      field: 'remark',
      formatter: ({ row }) => row.remark || '-',
      minWidth: 140,
      showOverflow: 'tooltip',
      title: $t('order.order.remark'),
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'orderNo',
          nameTitle: $t('order.order.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'deliver', text: $t('order.delivery.ship') },
          { code: 'detail', text: $t('order.order.detail') },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('order.order.operation'),
      width: 170,
    },
  ];
}
