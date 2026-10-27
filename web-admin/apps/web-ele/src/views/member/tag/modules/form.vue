<script lang="ts" setup>
import type { MemberTagApi } from '#/api/member/tag';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createTag, updateTag } from '#/api/member/tag';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MemberTagApi.Tag>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('member.tag.entity')])
    : $t('ui.actionTitle.create', [$t('member.tag.entity')]),
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
    const payload: MemberTagApi.TagPayload = {
      color: values.color,
      name: values.name,
      remark: values.remark,
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateTag(formData.value.id, payload)
        : createTag(payload));
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
    const data = drawerApi.getData<MemberTagApi.Tag>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        color: data.color,
        name: data.name,
        remark: data.remark,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[440px]">
    <Form />
  </Drawer>
</template>
