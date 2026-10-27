import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { SystemMenuApi } from '#/api/system/menu';

import { $t } from '#/locales';

/**
 * 菜单类型选项
 */
export function getMenuTypeOptions() {
  return [
    { color: 'processing', label: $t('system.menu.typeCatalog'), value: 'catalog' },
    { color: 'default', label: $t('system.menu.typeMenu'), value: 'menu' },
    { color: 'error', label: $t('system.menu.typeButton'), value: 'button' },
    { color: 'success', label: $t('system.menu.typeEmbedded'), value: 'embedded' },
    { color: 'warning', label: $t('system.menu.typeLink'), value: 'link' },
  ];
}

/**
 * 菜单表格列
 */
export function useColumns(
  onActionClick: OnActionClickFn<SystemMenuApi.SystemMenu>,
): VxeTableGridColumns<SystemMenuApi.SystemMenu> {
  return [
    {
      align: 'left',
      field: 'meta.title',
      fixed: 'left',
      slots: { default: 'title' },
      title: $t('system.menu.menuTitle'),
      treeNode: true,
      width: 240,
    },
    {
      align: 'center',
      cellRender: { name: 'CellTag', options: getMenuTypeOptions() },
      field: 'type',
      title: $t('system.menu.type'),
      width: 100,
    },
    {
      field: 'authCode',
      title: $t('system.menu.authCode'),
      width: 190,
    },
    {
      align: 'left',
      field: 'path',
      title: $t('system.menu.path'),
      width: 190,
    },
    {
      align: 'left',
      field: 'component',
      formatter: ({ row }) => {
        switch (row.type) {
          case 'catalog':
          case 'menu': {
            return row.component ?? '';
          }
          case 'embedded': {
            return row.meta?.iframeSrc ?? '';
          }
          case 'link': {
            return row.meta?.link ?? '';
          }
        }
        return '';
      },
      minWidth: 200,
      title: $t('system.menu.component'),
    },
    {
      cellRender: { name: 'CellTag' },
      field: 'status',
      title: $t('system.menu.status'),
      width: 90,
    },
    {
      align: 'right',
      cellRender: {
        attrs: {
          nameField: 'name',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          { code: 'append', text: $t('system.menu.append') },
          'edit',
          'delete',
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('system.menu.operation'),
      width: 200,
    },
  ];
}
