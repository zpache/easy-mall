import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { GoodsApi } from '#/api/goods/goods';

import { z } from '#/adapter/form';
import { getBrandOptions } from '#/api/goods/brand';
import { getCategoryTree } from '#/api/goods/category';
import GoodsImageField from '#/components/goods-image-field.vue';
import { $t } from '#/locales';

/**
 * 商品编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'ApiTreeSelect',
      componentProps: {
        api: getCategoryTree,
        childrenField: 'children',
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'categoryId',
      label: $t('goods.goods.category'),
      rules: 'selectRequired',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        allowClear: true,
        api: getBrandOptions,
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'brandId',
      label: $t('goods.goods.brand'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('goods.goods.name'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('goods.goods.name'), 2]))
        .max(100, $t('ui.formRules.maxLength', [$t('goods.goods.name'), 100])),
    },
    {
      component: 'Input',
      fieldName: 'subtitle',
      label: $t('goods.goods.subtitle'),
    },
    {
      component: GoodsImageField,
      fieldName: 'mainImage',
      help: $t('goods.goods.imageTip'),
      label: $t('goods.goods.mainImage'),
      rules: 'required',
    },
    {
      component: GoodsImageField,
      componentProps: { max: 5, multiple: true },
      fieldName: 'images',
      help: $t('goods.goods.imagesTip'),
      label: $t('goods.goods.images'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'price',
      label: $t('goods.goods.price'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0, precision: 2 },
      fieldName: 'originalPrice',
      label: $t('goods.goods.originalPrice'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      fieldName: 'stock',
      label: $t('goods.goods.stock'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 10,
      fieldName: 'stockWarn',
      label: $t('goods.goods.stockWarn'),
    },
    {
      component: 'Input',
      defaultValue: '件',
      fieldName: 'unit',
      label: $t('goods.goods.unit'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      fieldName: 'baseSales',
      help: $t('goods.goods.baseSalesTip'),
      label: $t('goods.goods.baseSales'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: $t('goods.goods.sort'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('goods.goods.onSale'), value: 1 },
          { label: $t('goods.goods.offSale'), value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: $t('goods.goods.status'),
    },
    {
      component: 'Textarea',
      componentProps: { rows: 4 },
      fieldName: 'detail',
      label: $t('goods.goods.detail'),
    },
  ];
}

/**
 * 商品查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { placeholder: $t('goods.goods.searchPlaceholder') },
      fieldName: 'keyword',
      label: $t('goods.goods.keyword'),
    },
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: getCategoryTree,
        childrenField: 'children',
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'categoryId',
      label: $t('goods.goods.category'),
    },
    {
      component: 'ApiSelect',
      componentProps: {
        allowClear: true,
        api: getBrandOptions,
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'brandId',
      label: $t('goods.goods.brand'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('goods.goods.onSale'), value: 1 },
          { label: $t('goods.goods.offSale'), value: 0 },
        ],
      },
      fieldName: 'status',
      label: $t('goods.goods.status'),
    },
  ];
}

/**
 * 商品表格列
 */
export function useColumns<T = GoodsApi.Goods>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      cellRender: {
        name: 'CellImage',
        props: { fit: 'cover', style: 'width:48px;height:48px' },
      },
      field: 'mainImage',
      title: $t('goods.goods.mainImage'),
      width: 90,
    },
    {
      field: 'name',
      minWidth: 200,
      title: $t('goods.goods.name'),
    },
    {
      field: 'subtitle',
      minWidth: 160,
      title: $t('goods.goods.subtitle'),
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
      field: 'price',
      formatter: ({ row }) =>
        row.price === null || row.price === undefined
          ? ''
          : `¥${Number(row.price).toFixed(2)}`,
      title: $t('goods.goods.price'),
      width: 110,
    },
    {
      field: 'stock',
      slots: { default: 'stock' },
      title: $t('goods.goods.stock'),
      width: 110,
    },
    {
      field: 'sales',
      title: $t('goods.goods.sales'),
      width: 90,
    },
    {
      field: 'baseSales',
      title: $t('goods.goods.baseSales'),
      width: 100,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('goods.goods.status'),
      width: 100,
    },
    {
      field: 'updatedAt',
      formatter: ({ row }) => formatTime(row.updatedAt),
      title: $t('goods.goods.updateTime'),
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('goods.goods.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('goods.goods.operation'),
      width: 150,
    },
  ];
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '';
}
