import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { GoodsStockApi } from '#/api/goods/stock';

import { $t } from '#/locales';

/**
 * 库存查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: $t('goods.stock.searchPlaceholder') },
      fieldName: 'keyword',
      label: $t('goods.stock.keyword'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('goods.stock.lowStock'), value: true },
          { label: $t('goods.stock.all'), value: false },
        ],
      },
      fieldName: 'lowStock',
      label: $t('goods.stock.stockStatus'),
    },
  ];
}

/**
 * 库存调整表单
 */
export function useAdjustSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'goodsName',
      label: $t('goods.stock.goods'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('goods.stock.typeIn'), value: 'in' },
          { label: $t('goods.stock.typeOut'), value: 'out' },
          { label: $t('goods.stock.typeCheck'), value: 'check' },
        ],
      },
      defaultValue: 'in',
      fieldName: 'type',
      label: $t('goods.stock.type'),
      rules: 'selectRequired',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      fieldName: 'qty',
      help: $t('goods.stock.qtyTip'),
      label: $t('goods.stock.qty'),
      rules: 'required',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: $t('goods.stock.remark'),
    },
  ];
}

/**
 * 库存表格列
 */
export function useColumns<T = GoodsStockApi.Stock>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  return [
    {
      cellRender: {
        name: 'CellImage',
        props: { fit: 'cover', style: 'width:44px;height:44px' },
      },
      field: 'mainImage',
      title: $t('goods.goods.mainImage'),
      width: 90,
    },
    {
      field: 'name',
      minWidth: 200,
      title: $t('goods.stock.goods'),
    },
    {
      field: 'categoryName',
      title: $t('goods.goods.category'),
      width: 120,
    },
    {
      field: 'brandName',
      title: $t('goods.goods.brand'),
      width: 120,
    },
    {
      field: 'stock',
      formatter: ({ row }) => `${row.stock} ${row.unit ?? ''}`,
      title: $t('goods.stock.stock'),
      width: 120,
    },
    {
      field: 'stockWarn',
      title: $t('goods.stock.stockWarn'),
      width: 110,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: [
          { color: 'error', label: $t('goods.stock.lowStock'), value: true },
          { color: 'success', label: $t('goods.stock.normal'), value: false },
        ],
      },
      field: 'lowStock',
      title: $t('goods.stock.stockStatus'),
      width: 110,
    },
    {
      field: 'updatedAt',
      formatter: ({ row }) =>
        row.updatedAt
          ? String(row.updatedAt).replace('T', ' ').slice(0, 19)
          : '',
      title: $t('goods.stock.updateTime'),
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('goods.stock.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'adjust', text: $t('goods.stock.adjust') },
          { code: 'logs', text: $t('goods.stock.logs') },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('goods.stock.operation'),
      width: 190,
    },
  ];
}

/**
 * 库存流水表格列
 */
export function useLogColumns(): VxeTableGridColumns {
  const typeMap: Record<string, string> = {
    check: $t('goods.stock.typeCheck'),
    in: $t('goods.stock.typeIn'),
    out: $t('goods.stock.typeOut'),
  };
  return [
    {
      field: 'type',
      formatter: ({ row }) => typeMap[row.type] ?? row.type,
      title: $t('goods.stock.type'),
      width: 100,
    },
    {
      field: 'changeQty',
      formatter: ({ row }) =>
        row.changeQty > 0 ? `+${row.changeQty}` : `${row.changeQty}`,
      title: $t('goods.stock.changeQty'),
      width: 100,
    },
    {
      field: 'beforeStock',
      title: $t('goods.stock.beforeStock'),
      width: 100,
    },
    {
      field: 'afterStock',
      title: $t('goods.stock.afterStock'),
      width: 100,
    },
    {
      field: 'remark',
      minWidth: 160,
      title: $t('goods.stock.remark'),
    },
    {
      field: 'createdAt',
      formatter: ({ row }) =>
        row.createdAt
          ? String(row.createdAt).replace('T', ' ').slice(0, 19)
          : '',
      title: $t('goods.stock.time'),
      width: 180,
    },
  ];
}
