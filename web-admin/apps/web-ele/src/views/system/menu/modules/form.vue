<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { SystemMenuApi } from '#/api/system/menu';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm, z } from '#/adapter/form';
import {
  createMenu,
  getMenuList,
  isMenuNameExists,
  isMenuPathExists,
  toTreeOptions,
  updateMenu,
} from '#/api/system/menu';
import { $t } from '#/locales';

import { getMenuTypeOptions } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | SystemMenuApi.SystemMenu>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('system.menu.name')])
    : $t('ui.actionTitle.create', [$t('system.menu.name')]),
);

async function loadMenuOptions() {
  return toTreeOptions(await getMenuList());
}

function useSchema(): VbenFormSchema[] {
  return [
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: getMenuTypeOptions(),
      },
      defaultValue: 'menu',
      fieldName: 'type',
      formItemClass: 'col-span-2',
      label: $t('system.menu.type'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('system.menu.menuName'),
      // 注意：不能用 z.refine(async ...)——vben 表单渲染时会同步调用
      // rules.isOptional()，异步 refine 会抛错打断渲染（parentNode 报错）。
      // 唯一性校验移到提交时执行，见 onConfirm。
      rules: z
        .string()
        .min(2, $t('ui.formRules.minLength', [$t('system.menu.menuName'), 2]))
        .max(30, $t('ui.formRules.maxLength', [$t('system.menu.menuName'), 30])),
    },
    {
      component: 'Input',
      fieldName: 'meta.title',
      label: $t('system.menu.menuTitle'),
      rules: 'required',
    },
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: loadMenuOptions,
        checkStrictly: true,
        childrenField: 'children',
        class: 'w-full',
        labelField: 'label',
        valueField: 'value',
      },
      fieldName: 'pid',
      label: $t('system.menu.parentMenu'),
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => values.type !== 'button',
        triggerFields: ['type'],
      },
      fieldName: 'path',
      label: $t('system.menu.path'),
      rules: z.string().max(100).optional(),
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => ['catalog', 'menu'].includes(values.type),
        triggerFields: ['type'],
      },
      fieldName: 'component',
      label: $t('system.menu.component'),
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => ['button', 'menu'].includes(values.type),
        triggerFields: ['type'],
      },
      fieldName: 'authCode',
      label: $t('system.menu.authCode'),
    },
    {
      component: 'IconPicker',
      componentProps: { class: 'w-full' },
      fieldName: 'meta.icon',
      label: $t('system.menu.icon'),
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', min: 0 },
      defaultValue: 0,
      fieldName: 'meta.order',
      label: $t('system.menu.order'),
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => values.type === 'link',
        triggerFields: ['type'],
      },
      fieldName: 'meta.link',
      label: $t('system.menu.link'),
    },
    {
      component: 'Input',
      dependencies: {
        show: (values) => values.type === 'embedded',
        triggerFields: ['type'],
      },
      fieldName: 'meta.iframeSrc',
      label: $t('system.menu.iframeSrc'),
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
      label: $t('system.menu.status'),
    },
    {
      component: 'Switch',
      defaultValue: false,
      fieldName: 'meta.keepAlive',
      label: $t('system.menu.keepAlive'),
    },
    {
      component: 'Switch',
      defaultValue: false,
      fieldName: 'meta.affixTab',
      label: $t('system.menu.affixTab'),
    },
    {
      component: 'Switch',
      defaultValue: false,
      fieldName: 'meta.hideInMenu',
      label: $t('system.menu.hideInMenu'),
    },
    {
      component: 'Switch',
      defaultValue: false,
      fieldName: 'meta.hideChildrenInMenu',
      label: $t('system.menu.hideChildrenInMenu'),
    },
    {
      component: 'Switch',
      defaultValue: false,
      fieldName: 'meta.hideInBreadcrumb',
      label: $t('system.menu.hideInBreadcrumb'),
    },
    {
      component: 'Switch',
      defaultValue: false,
      fieldName: 'meta.hideInTab',
      label: $t('system.menu.hideInTab'),
    },
  ];
}

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  layout: 'vertical',
  schema: useSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-4',
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    // 唯一性校验（提交时执行，表单渲染阶段不能跑异步校验）
    if (
      await isMenuNameExists(values.name as string, formData.value?.id)
    ) {
      ElMessage.error(
        $t('ui.formRules.alreadyExists', [
          $t('system.menu.menuName'),
          values.name,
        ]),
      );
      return;
    }
    if (
      values.path &&
      (await isMenuPathExists(values.path as string, formData.value?.id))
    ) {
      ElMessage.error(
        $t('ui.formRules.alreadyExists', [$t('system.menu.path'), values.path]),
      );
      return;
    }
    const meta = values.meta ?? {};
    const payload = {
      authCode: values.authCode,
      component: values.component,
      meta: {
        affixTab: !!meta.affixTab,
        hideChildrenInMenu: !!meta.hideChildrenInMenu,
        hideInBreadcrumb: !!meta.hideInBreadcrumb,
        hideInMenu: !!meta.hideInMenu,
        hideInTab: !!meta.hideInTab,
        icon: meta.icon,
        iframeSrc: meta.iframeSrc,
        keepAlive: !!meta.keepAlive,
        link: meta.link,
        order: meta.order ?? 0,
        title: meta.title,
      },
      name: values.name,
      path: values.path,
      pid: values.pid ?? 0,
      status: values.status ?? 1,
      type: values.type,
    };
    drawerApi.lock();
    try {
      await (formData.value?.id
        ? updateMenu(formData.value.id, payload)
        : createMenu(payload));
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<SystemMenuApi.SystemMenu>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        authCode: data.authCode,
        component: data.component,
        meta: data.meta ?? {},
        name: data.name,
        path: data.path,
        pid: data.pid === 0 ? undefined : data.pid,
        status: data.status ?? 1,
        type: data.type,
      });
    } else if (data?.pid) {
      formApi.setValues({ pid: data.pid });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[680px]">
    <Form />
  </Drawer>
</template>
