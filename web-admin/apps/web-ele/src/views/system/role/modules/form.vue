<script lang="ts" setup>
import type { SystemMenuApi } from '#/api/system/menu';
import type { SystemRoleApi } from '#/api/system/role';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElTree } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { getMenuList } from '#/api/system/menu';
import { createRole, updateRole } from '#/api/system/role';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

interface MenuNode {
  children?: MenuNode[];
  id: number;
  title: string;
}

const formData = ref<null | SystemRoleApi.SystemRole>(null);
const menuTree = ref<MenuNode[]>([]);
const treeRef = ref<InstanceType<typeof ElTree>>();

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('system.role.name')])
    : $t('ui.actionTitle.create', [$t('system.role.name')]),
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    drawerApi.lock();
    const permissions = [
      ...(treeRef.value?.getCheckedKeys() ?? []),
      ...(treeRef.value?.getHalfCheckedKeys() ?? []),
    ].map((key) => Number(key));
    const payload = {
      code: values.code,
      name: values.name,
      permissions,
      remark: values.remark,
      sort: values.sort ?? 0,
      status: values.status ?? 1,
    };
    try {
      await (formData.value?.id
        ? updateRole(formData.value.id, payload)
        : createRole(payload));
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<SystemRoleApi.SystemRole>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (menuTree.value.length === 0) {
      menuTree.value = toMenuNodes(await getMenuList());
    }
    await nextTick();
    if (data?.id) {
      formApi.setValues({
        code: data.code,
        name: data.name,
        remark: data.remark,
        sort: data.sort,
        status: data.status ?? 1,
      });
    }
    // 先清空再回显，避免上一次勾选残留
    treeRef.value?.setCheckedKeys([]);
    if (data?.permissions?.length) {
      treeRef.value?.setCheckedKeys(data.permissions, false);
    }
  },
});

function toMenuNodes(list: SystemMenuApi.SystemMenu[]): MenuNode[] {
  return list.map((item) => ({
    children:
      item.children && item.children.length > 0
        ? toMenuNodes(item.children)
        : undefined,
    id: item.id,
    title: item.meta?.title ?? item.name,
  }));
}
</script>

<template>
  <Drawer :title="getTitle" class="w-[560px]">
    <Form />
    <div class="mt-2 px-4">
      <div class="mb-2 text-sm font-medium">
        {{ $t('system.role.setPermissions') }}
      </div>
      <div class="text-muted-foreground mb-2 text-xs">
        {{ $t('system.role.permissionTip') }}
      </div>
      <ElTree
        ref="treeRef"
        :data="menuTree"
        :props="{ children: 'children', label: 'title' }"
        class="max-h-[320px] overflow-auto rounded border p-2"
        default-expand-all
        node-key="id"
        show-checkbox
      />
    </div>
  </Drawer>
</template>
