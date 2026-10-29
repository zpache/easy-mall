import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MemberApi } from '#/api/member/member';

import { reactive } from 'vue';

import { $t } from '#/locales';

import { useSourceOptions } from './options';

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

export function formatMoney(value?: number | string) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return `¥${Number(value).toFixed(2)}`;
}

/** 等级选项（页面加载时注入，等级可增删） */
export const levelOptionList = reactive<{ label: string; value: number }[]>(
  [],
);

export function setLevelOptions(
  options: { label: string; value: number }[],
): void {
  levelOptionList.splice(0, levelOptionList.length, ...options);
}

/**
 * 会员查询表单：关键词 / 等级 / 来源渠道 / 标签
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('member.list.searchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('member.list.keyword'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: levelOptionList,
        placeholder: `${$t('member.list.level')}…`,
      },
      fieldName: 'levelId',
      label: $t('member.list.level'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: useSourceOptions(),
      },
      fieldName: 'source',
      label: $t('member.list.source'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: tagOptionList,
        placeholder: `${$t('member.list.tag')}…`,
      },
      fieldName: 'tagId',
      label: $t('member.list.tag'),
    },
  ];
}

/** 标签选项（页面加载时注入） */
export const tagOptionList = reactive<{ label: string; value: number }[]>([]);

export function setTagOptions(
  options: { label: string; value: number }[],
): void {
  tagOptionList.splice(0, tagOptionList.length, ...options);
}

/**
 * 会员表格列
 */
export function useColumns<T = MemberApi.Member>(
  onActionClick: OnActionClickFn<T>,
): VxeTableGridColumns {
  return [
    {
      field: 'avatar',
      minWidth: 80,
      // 头像用插槽渲染：微信头像可能是任意域名，加载失败时回退昵称首字
      slots: { default: 'avatar' },
      title: $t('member.list.avatar'),
      width: 80,
    },
    {
      field: 'nickname',
      minWidth: 140,
      title: $t('member.list.nickname'),
    },
    {
      field: 'phone',
      title: $t('member.list.phone'),
      width: 130,
    },
    {
      field: 'levelName',
      title: $t('member.list.level'),
      width: 100,
    },
    {
      cellRender: {
        name: 'CellTag',
        options: useSourceOptions(),
      },
      field: 'source',
      title: $t('member.list.source'),
      width: 100,
    },
    {
      field: 'points',
      title: $t('member.list.points'),
      width: 90,
    },
    {
      field: 'totalSpend',
      formatter: ({ row }) => formatMoney(row.totalSpend),
      title: $t('member.list.totalSpend'),
      width: 110,
    },
    {
      field: 'orderCount',
      title: $t('member.list.orderCount'),
      width: 90,
    },
    {
      field: 'tags',
      formatter: ({ row }) =>
        (row.tags as MemberApi.TagItem[])?.map((tag) => tag.name).join('、') ||
        '-',
      minWidth: 160,
      title: $t('member.list.tag'),
    },
    {
      field: 'createdAt',
      formatter: ({ row }) => formatTime(row.createdAt),
      title: $t('member.list.createdAt'),
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'nickname',
          nameTitle: $t('member.list.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'points', text: $t('member.list.adjustPoints') },
          { code: 'tags', text: $t('member.list.setTag') },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('member.list.operation'),
      width: 160,
    },
  ];
}
