import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MarketingSigninApi } from '#/api/marketing/signin';

import { z } from '#/adapter/form';
import { $t } from '#/locales';

export function formatTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

/**
 * 签到规则编辑表单
 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('marketing.signin.ruleName'),
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('marketing.signin.ruleName'), 2]))
        .max(100, $t('ui.formRules.maxLength', [$t('marketing.signin.ruleName'), 100])),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', max: 1000, min: 1 },
      defaultValue: 1,
      fieldName: 'pointsPerDay',
      label: $t('marketing.signin.pointsPerDay'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', max: 365, min: 0 },
      defaultValue: 0,
      fieldName: 'bonusEveryDays',
      help: $t('marketing.signin.bonusEveryDaysTip'),
      label: $t('marketing.signin.bonusEveryDays'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 1 },
      defaultValue: 10,
      dependencies: {
        if: (values) => Number(values.bonusEveryDays) > 0,
        triggerFields: ['bonusEveryDays'],
      },
      fieldName: 'bonusPoints',
      label: $t('marketing.signin.bonusPoints'),
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
      label: $t('marketing.signin.status'),
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: $t('marketing.signin.remark'),
    },
  ];
}

/**
 * 签到规则查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('marketing.signin.ruleSearchPlaceholder'),
      },
      fieldName: 'keyword',
      label: $t('marketing.signin.ruleName'),
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
      label: $t('marketing.signin.status'),
    },
  ];
}

/**
 * 签到规则表格列
 */
export function useColumns<T = MarketingSigninApi.SigninRule>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    { field: 'name', minWidth: 160, title: $t('marketing.signin.ruleName') },
    { field: 'pointsPerDay', title: $t('marketing.signin.pointsPerDay'), width: 110 },
    {
      field: 'bonusEveryDays',
      formatter: ({ row }) =>
        Number(row.bonusEveryDays) > 0
          ? $t('marketing.signin.bonusInfo', [row.bonusEveryDays, row.bonusPoints])
          : '-',
      minWidth: 200,
      title: $t('marketing.signin.bonusEveryDays'),
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('marketing.signin.status'),
      width: 90,
    },
    { field: 'remark', minWidth: 220, title: $t('marketing.signin.remark') },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'name',
          nameTitle: $t('marketing.signin.entity'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: ['edit', 'delete'],
      },
      field: 'operation',
      fixed: 'right',
      title: $t('marketing.signin.operation'),
      width: 150,
    },
  ];
}

/**
 * 签到记录表格列
 */
export function useLogColumns(): VxeTableGridColumns {
  return [
    { field: 'nickname', title: $t('marketing.signin.logUser'), width: 120 },
    { field: 'signDate', title: $t('marketing.signin.signDate'), width: 120 },
    {
      field: 'continuousDays',
      title: $t('marketing.signin.continuousDays'),
      width: 110,
    },
    {
      field: 'points',
      formatter: ({ row }) => `+${row.points}`,
      title: $t('marketing.signin.logPoints'),
      width: 100,
    },
    {
      field: 'createdAt',
      formatter: ({ row }) => formatTime(row.createdAt),
      title: $t('marketing.signin.logTime'),
      width: 170,
    },
  ];
}
