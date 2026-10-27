<script lang="ts" setup>
import type { SystemUserApi } from '#/api/system/user';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createUser, updateUser } from '#/api/system/user';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | SystemUserApi.SystemUser>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('system.user.name')])
    : $t('ui.actionTitle.create', [$t('system.user.name')]),
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
    if (!formData.value?.id && !values.password) {
      ElMessage.error('新增用户必须设置初始密码');
      return;
    }
    drawerApi.lock();
    const payload = {
      deptId: values.deptId,
      email: values.email,
      nickname: values.nickname,
      password: values.password || undefined,
      phone: values.phone,
      roleIds: values.roleIds ?? [],
      status: values.status ?? 1,
      username: values.username,
    };
    try {
      await (formData.value?.id
        ? updateUser(formData.value.id, payload)
        : createUser(payload));
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
    const data = drawerApi.getData<SystemUserApi.SystemUser>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        deptId: data.deptId,
        email: data.email,
        nickname: data.nickname,
        phone: data.phone,
        roleIds: data.roleIds ?? [],
        status: data.status ?? 1,
        username: data.username,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
