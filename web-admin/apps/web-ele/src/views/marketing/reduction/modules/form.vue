<script lang="ts" setup>
import type { MarketingReductionApi } from '#/api/marketing/reduction';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createReduction, updateReduction } from '#/api/marketing/reduction';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MarketingReductionApi.Reduction>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('marketing.reduction.edit')
    : $t('marketing.reduction.create'),
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
    const payload: MarketingReductionApi.ReductionPayload = {
      benefit: Number(values.benefit),
      endTime: values.endTime,
      mode: Number(values.mode),
      name: values.name,
      startTime: values.startTime,
      status: values.status ?? 1,
      threshold: Number(values.threshold),
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateReduction(formData.value.id, payload)
        : createReduction(payload));
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
    const data = drawerApi.getData<MarketingReductionApi.Reduction>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        benefit: Number(data.benefit),
        endTime: data.endTime,
        mode: data.mode,
        name: data.name,
        startTime: data.startTime,
        status: data.status ?? 1,
        threshold: Number(data.threshold),
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
