import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/order/order';

import { $t } from '#/locales';

/**
 * 订单状态标签配置
 */
export function useStatusOptions() {
  return [
    { color: 'warning', label: $t('order.order.statusUnpaid'), value: 0 },
    {
      color: 'processing',
      label: $t('order.order.statusUndelivered'),
      value: 1,
    },
    { color: 'default', label: $t('order.order.statusUnreceived'), value: 2 },
    { color: 'success', label: $t('order.order.statusFinished'), value: 3 },
    { color: 'error', label: $t('order.order.statusCancelled'), value: 4 },
  ];
}

/**
 * 售后状态标签配置
 */
export function useRefundStatusOptions() {
  return [
    { color: 'default', label: $t('order.order.refundNone'), value: 0 },
    {
      color: 'processing',
      label: $t('order.order.refundProcessing'),
      value: 1,
    },
    { color: 'success', label: $t('order.order.refundDone'), value: 2 },
    { color: 'error', label: $t('order.order.refundRejected'), value: 3 },
  ];
}

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

export function formatMoney(value?: number | string) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return `¥${Number(value).toFixed(2)}`;
}

/**
 * 列表中的商品摘要：首件商品名 + 共 N 件
 */
export function summaryGoods(row: OrderApi.Order) {
  const first = row.items?.[0];
  if (!first) {
    return $t('order.order.emptyItems');
  }
  return row.items.length > 1
    ? `${first.goodsName} ${$t('order.order.andMore', [row.itemCount])}`
    : first.goodsName;
}

/**
 * 订单查询表单
 */
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
      componentProps: { allowClear: true, options: useStatusOptions() },
      fieldName: 'status',
      label: $t('order.order.status'),
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
 * 订单表格列
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
      minWidth: 240,
      title: $t('order.order.goods'),
    },
    {
      field: 'userNickname',
      formatter: ({ row }) => row.userNickname || $t('order.order.buyerEmpty'),
      title: $t('order.order.buyer'),
      width: 130,
    },
    {
      field: 'payAmount',
      formatter: ({ row }) => formatMoney(row.payAmount),
      title: $t('order.order.amount'),
      width: 120,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: useStatusOptions(),
      },
      field: 'status',
      title: $t('order.order.status'),
      width: 110,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: useRefundStatusOptions(),
      },
      field: 'refundStatus',
      title: $t('order.order.refundStatus'),
      width: 110,
    },
    {
      field: 'createdAt',
      formatter: ({ row }) => formatTime(row.createdAt),
      title: $t('order.order.createdAt'),
      width: 170,
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
          { code: 'detail', text: $t('order.order.detail') },
          {
            code: 'deliver',
            show: (row: OrderApi.Order) => row.status === 1,
            text: $t('order.order.deliver'),
          },
          {
            code: 'close',
            show: (row: OrderApi.Order) => row.status === 0 || row.status === 1,
            text: $t('order.order.close'),
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('order.order.operation'),
      width: 200,
    },
  ];
}
