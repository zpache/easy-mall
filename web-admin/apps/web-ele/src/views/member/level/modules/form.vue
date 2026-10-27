<script lang="ts" setup>
import type { MemberLevelApi } from '#/api/member/level';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createLevel, updateLevel } from '#/api/member/level';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MemberLevelApi.Level>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('member.level.entity')])
    : $t('ui.actionTitle.create', [$t('member.level.entity')]),
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
    const payload: MemberLevelApi.LevelPayload = {
      description: values.description,
      discount: Number(values.discount),
      name: values.name,
      sort: values.sort,
      status: values.status ?? 1,
      threshold: Number(values.threshold),
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateLevel(formData.value.id, payload)
        : createLevel(payload));
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
    const data = drawerApi.getData<MemberLevelApi.Level>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        description: data.description,
        discount: data.discount,
        name: data.name,
        sort: data.sort ?? 0,
        status: data.status ?? 1,
        threshold: data.threshold,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[480px]">
    <Form />
  </Drawer>
</template>
