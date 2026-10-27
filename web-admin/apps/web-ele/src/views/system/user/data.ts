import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { SystemUserApi } from '#/api/system/user';

import { z } from '#/adapter/form';
import { getDeptList } from '#/api/system/dept';
import { getRoleOptions } from '#/api/system/role';
import { $t } from '#/locales';

/**
 * 用户编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { disabled: false },
      fieldName: 'username',
      label: $t('system.user.username'),
      rules: z
        .string()
        .min(4, $t('ui.formRules.minLength', [$t('system.user.username'), 4]))
        .max(32, $t('ui.formRules.maxLength', [$t('system.user.username'), 32]))
        .regex(/^[a-zA-Z0-9_]+$/, '只能包含字母、数字和下划线'),
    },
    {
      component: 'Input',
      componentProps: { autocomplete: 'new-password', type: 'password' },
      fieldName: 'password',
      help: $t('system.user.passwordTip'),
      label: $t('system.user.password'),
    },
    {
      component: 'Input',
      fieldName: 'nickname',
      label: $t('system.user.nickname'),
      rules: 'required',
    },
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: getDeptList,
        checkStrictly: true,
        childrenField: 'children',
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'deptId',
      label: $t('system.user.dept'),
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getRoleOptions,
        class: 'w-full',
        labelField: 'name',
        multiple: true,
        valueField: 'id',
      },
      fieldName: 'roleIds',
      label: $t('system.user.roles'),
    },
    {
      component: 'Input',
      fieldName: 'phone',
      label: $t('system.user.phone'),
      rules: z
        .string()
        .regex(/^$|^1[3-9]\d{9}$/, '手机号格式不正确')
        .optional(),
    },
    {
      component: 'Input',
      fieldName: 'email',
      label: $t('system.user.email'),
      rules: z.string().email('邮箱格式不正确').optional(),
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
      label: $t('system.user.status'),
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
      componentProps: { placeholder: $t('system.user.searchPlaceholder') },
      fieldName: 'keyword',
      label: $t('system.user.keyword'),
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
      label: $t('system.user.status'),
    },
  ];
}

/**
 * 用户表格列
 */
export function useColumns<T = SystemUserApi.SystemUser>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      field: 'username',
      minWidth: 150,
      title: $t('system.user.username'),
    },
    {
      field: 'nickname',
      minWidth: 140,
      title: $t('system.user.nickname'),
    },
    {
      field: 'deptName',
      minWidth: 140,
      title: $t('system.user.dept'),
    },
    {
      field: 'roleNames',
      formatter: ({ row }) =>
        Array.isArray(row.roleNames) ? row.roleNames.join('、') : '',
      minWidth: 140,
      title: $t('system.user.roles'),
    },
    {
      field: 'phone',
      title: $t('system.user.phone'),
      width: 130,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('system.user.status'),
      width: 100,
    },
    {
      field: 'createdAt',
      title: $t('system.user.createTime'),
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'nickname',
          nameTitle: $t('system.user.name'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          'edit',
          { code: 'resetPassword', text: '重置密码' },
          'delete',
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('system.user.operation'),
      width: 220,
    },
  ];
}
