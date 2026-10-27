import type { Recordable } from '@vben/types';
import type { VxeTableGridOptions } from '@vben/plugins/vxe-table';

import type { ComponentPropsMap, ComponentType } from './component';

import { h } from 'vue';

import { $t } from '@vben/locales';
import {
  setupVbenVxeTable,
  useVbenVxeGrid as useGrid,
} from '@vben/plugins/vxe-table';

import { ElButton, ElImage, ElPopconfirm, ElSwitch, ElTag } from 'element-plus';

import { useVbenForm } from './form';

setupVbenVxeTable({
  configVxeTable: (vxeUI) => {
    vxeUI.setConfig({
      grid: {
        align: 'center',
        border: false,
        columnConfig: {
          resizable: true,
        },
        minHeight: 180,
        formConfig: {
          // 全局禁用vxe-table的表单配置，使用formOptions
          enabled: false,
        },
        proxyConfig: {
          autoLoad: true,
          response: {
            result: 'items',
            total: 'total',
            list: 'items',
          },
          showActiveMsg: true,
          showResponseMsg: false,
        },
        round: true,
        showOverflow: true,
        size: 'small',
      } as VxeTableGridOptions,
    });

    // 表格配置项可以用 cellRender: { name: 'CellImage' },
    vxeUI.renderer.add('CellImage', {
      renderTableDefault(renderOpts, params) {
        const { props } = renderOpts;
        const { column, row } = params;
        const src = row[column.field];
        return h(ElImage, { src, previewSrcList: [src], ...props });
      },
    });

    // 表格配置项可以用 cellRender: { name: 'CellLink' },
    vxeUI.renderer.add('CellLink', {
      renderTableDefault(renderOpts) {
        const { props } = renderOpts;
        return h(
          ElButton,
          { size: 'small', link: true },
          { default: () => props?.text },
        );
      },
    });

    // 状态标签：0 禁用 / 1 启用，也支持传入自定义 options
    vxeUI.renderer.add('CellTag', {
      renderTableDefault(renderOpts, { column, row }) {
        const { options, props } = renderOpts;
        const value = row[column.field];
        const tagTypeMap: Recordable<string> = {
          default: 'info',
          error: 'danger',
          processing: 'primary',
          success: 'success',
          warning: 'warning',
        };
        const presets: Recordable<Recordable<any>> = {
          0: { label: $t('common.disabled'), type: 'info' },
          1: { label: $t('common.enabled'), type: 'success' },
        };
        const matched: Recordable<any> | undefined = (options ?? []).find(
          (item: Recordable<any>) => item.value === value,
        );
        const config = matched ?? presets[String(value)] ?? { label: value };
        const tagType = config.color
          ? tagTypeMap[config.color]
          : (config.type ?? 'info');
        return h(
          ElTag,
          { effect: 'plain', type: tagType as any, ...props },
          { default: () => config.label },
        );
      },
    });

    // 状态开关：切换前可拦截（beforeChange 返回 false 则中止）
    vxeUI.renderer.add('CellSwitch', {
      renderTableDefault({ attrs = {}, props = {} }, { column, row }) {
        return h(ElSwitch, {
          ...props,
          modelValue: row[column.field] === 1,
          'onUpdate:modelValue': (checked: boolean | number | string) => {
            row[column.field] = checked ? 1 : 0;
          },
          async beforeChange() {
            if (typeof attrs.beforeChange !== 'function') {
              return true;
            }
            const next = row[column.field] === 1 ? 0 : 1;
            const result = await attrs.beforeChange(next, row);
            return result !== false;
          },
        });
      },
    });

    // 行操作按钮组：options 传 'edit' / 'delete' 或 { code, text }
    vxeUI.renderer.add('CellOperation', {
      renderTableDefault({ attrs = {}, options, props = {} }, { row }) {
        const presets: Recordable<Recordable<any>> = {
          delete: { danger: true, text: $t('common.delete') },
          edit: { text: $t('common.edit') },
        };
        const operations: Recordable<any>[] = (options ?? ['edit', 'delete'])
          .map((option: any) => {
            if (typeof option === 'string') {
              return presets[option]
                ? { code: option, ...presets[option] }
                : { code: option, text: option };
            }
            return { ...presets[option.code], ...option };
          })
          .map((option) => {
            const resolved: Recordable<any> = {};
            Object.keys(option).forEach((key) => {
              resolved[key] =
                typeof option[key] === 'function'
                  ? option[key](row)
                  : option[key];
            });
            return resolved;
          })
          .filter((option) => option.show !== false);

        const renderButton = (option: Recordable<any>, listen = true) =>
          h(
            ElButton,
            {
              ...props,
              danger: option.danger,
              link: true,
              size: 'small',
              type: option.danger ? 'danger' : 'primary',
              ...(listen
                ? {
                    onClick: () =>
                      attrs?.onClick?.({ code: option.code, row }),
                  }
                : {}),
            },
            { default: () => option.text },
          );

        const buttons = operations.map((option) =>
          option.code === 'delete'
            ? h(
                ElPopconfirm,
                {
                  title: $t('ui.actionMessage.deleteConfirm', [
                    row[attrs?.nameField ?? 'name'],
                  ]),
                  width: 220,
                  onConfirm: () => attrs?.onClick?.({ code: option.code, row }),
                },
                { reference: () => renderButton(option, false) },
              )
            : renderButton(option),
        );

        return h(
          'div',
          { class: 'flex items-center justify-center gap-1' },
          buttons,
        );
      },
    });

    // 这里可以自行扩展 vxe-table 的全局配置，比如自定义格式化
    // vxeUI.formats.add
  },
  useVbenForm,
});

export const useVbenVxeGrid = <T extends Record<string, any>>(
  ...rest: Parameters<typeof useGrid<T, ComponentType, ComponentPropsMap>>
) => useGrid<T, ComponentType, ComponentPropsMap>(...rest);

export type * from '@vben/plugins/vxe-table';

/**
 * 行操作按钮点击参数 / 回调类型。
 */
export type OnActionClickParams<T = Recordable<any>> = {
  code: string;
  row: T;
};

export type OnActionClickFn<T = Recordable<any>> = (
  params: OnActionClickParams<T>,
) => void;
