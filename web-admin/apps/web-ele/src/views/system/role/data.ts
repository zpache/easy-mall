import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { SystemRoleApi } from '#/api/system/role';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

/**
 * 角色编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('system.role.roleName'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('system.role.roleName'), 2]))
        .max(50, $t('ui.formRules.maxLength', [$t('system.role.roleName'), 50])),
    },
    {
      component: 'Input',
      fieldName: 'code',
      help: '大写字母、数字、下划线，作为 Spring Security 的 ROLE_ 前缀',
      label: $t('system.role.code'),
      rules: z
        .string()
        .regex(
          /^[A-Z][A-Z0-9_]{1,49}$/,
          '需为大写字母、数字或下划线，且以字母开头',
        ),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'sort',
      label: $t('system.role.sort'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('common.enabled'), value: 1 },
          { label: $t('common.disabled'), value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: $t('system.role.status'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 2, showCount: true },
      fieldName: 'remark',
      label: $t('system.role.remark'),
    },
  ];
}

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('system.role.roleName'),
    },
    {
      component: 'Input',
      fieldName: 'code',
      label: $t('system.role.code'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: $t('common.enabled'), value: 1 },
          { label: $t('common.disabled'), value: 0 },
        ],
      },
      fieldName: 'status',
      label: $t('system.role.status'),
    },
  ];
}

/**
 * 角色表格列
 */
export function useColumns<T = SystemRoleApi.SystemRole>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      field: 'name',
      minWidth: 160,
      title: $t('system.role.roleName'),
    },
    {
      field: 'code',
      title: $t('system.role.code'),
      width: 160,
    },
    {
      field: 'sort',
      title: $t('system.role.sort'),
      width: 90,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('system.role.status'),
      width: 100,
    },
    {
      field: 'remark',
      minWidth: 140,
      title: $t('system.role.remark'),
    },
    {
      field: 'createdAt',
      title: $t('system.role.createTime'),
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('system.role.name'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
      },
      field: 'operation',
      fixed: 'right',
      title: $t('system.role.operation'),
      width: 140,
    },
  ];
}
