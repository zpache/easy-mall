import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OrderCommentApi } from '#/api/order/comment';

import { $t } from '#/locales';

import { formatTime } from '../list/data';

function scoreLabel(score: number) {
  const value = Math.max(0, Math.min(5, Number(score) || 0));
  return `${'★'.repeat(value)}${'☆'.repeat(5 - value)}`;
}

/**
 * 审核状态标签配置
 */
export function useAuditStatusOptions() {
  return [
    { color: 'warning', label: $t('order.comment.auditPending'), value: 0 },
    { color: 'success', label: $t('order.comment.auditPassed'), value: 1 },
    { color: 'error', label: $t('order.comment.auditRejected'), value: 2 },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('order.comment.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('order.comment.keyword'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useAuditStatusOptions().map(({ label, value }) => ({
          label,
          value,
        })),
      },
      fieldName: 'auditStatus',
      label: $t('order.comment.auditStatus'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [1, 2, 3, 4, 5].map((score) => ({
          label: scoreLabel(score),
          value: score,
        })),
      },
      fieldName: 'score',
      label: $t('order.comment.score'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('order.comment.show'), value: 1 },
          { label: $t('order.comment.hide'), value: 0 },
        ],
      },
      fieldName: 'isShow',
      label: $t('order.comment.isShow'),
    },
  ];
}

export function useColumns<T = OrderCommentApi.Comment>(
  onActionClick: OnActionClickFn<T>,
  onVisibleChange: (newValue: number, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      cellRender: {
        name: 'CellImage',
        props: { fit: 'cover', style: 'width:40px;height:40px' },
      },
      field: 'goodsImage',
      title: $t('order.comment.goods'),
      width: 80,
    },
    {
      field: 'goodsName',
      minWidth: 180,
      title: $t('order.comment.goods'),
    },
    {
      cellRender: {
        name: 'CellTag',
        options: useAuditStatusOptions(),
      },
      field: 'auditStatus',
      title: $t('order.comment.auditStatus'),
      width: 110,
    },
    {
      field: 'score',
      formatter: ({ row }) => scoreLabel(row.score),
      title: $t('order.comment.score'),
      width: 130,
    },
    {
      field: 'content',
      minWidth: 240,
      showOverflow: 'tooltip',
      title: $t('order.comment.content'),
    },
    {
      field: 'userNickname',
      formatter: ({ row }) => row.userNickname || $t('order.order.buyerEmpty'),
      title: $t('order.comment.user'),
      width: 120,
    },
    {
      field: 'orderNo',
      minWidth: 170,
      title: $t('order.comment.orderNo'),
    },
    {
      field: 'reply',
      formatter: ({ row }) => row.reply || '-',
      minWidth: 180,
      showOverflow: 'tooltip',
      title: $t('order.comment.reply'),
    },
    {
      cellRender: {
        attrs: { beforeChange: onVisibleChange },
        name: 'CellSwitch',
      },
      field: 'isShow',
      title: $t('order.comment.isShow'),
      width: 110,
    },
    {
      field: 'commentedAt',
      formatter: ({ row }) => formatTime(row.commentedAt),
      title: $t('order.comment.commentedAt'),
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'goodsName',
          nameTitle: $t('order.comment.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'audit', text: $t('order.comment.auditAction') },
          { code: 'reply', text: $t('order.comment.replyAction') },
          { code: 'delete', text: $t('order.comment.delete') },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('order.comment.operation'),
      width: 200,
    },
  ];
}
