<script lang="ts" setup>
import type { MarketingSigninApi } from '#/api/marketing/signin';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createSigninRule, updateSigninRule } from '#/api/marketing/signin';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MarketingSigninApi.SigninRule>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('marketing.signin.edit')
    : $t('marketing.signin.create'),
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
    const payload: MarketingSigninApi.SigninRulePayload = {
      bonusEveryDays: Number(values.bonusEveryDays ?? 0),
      bonusPoints:
        Number(values.bonusEveryDays ?? 0) > 0 ? Number(values.bonusPoints) : 0,
      name: values.name,
      pointsPerDay: Number(values.pointsPerDay),
      remark: values.remark,
      status: values.status ?? 1,
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateSigninRule(formData.value.id, payload)
        : createSigninRule(payload));
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
    const data = drawerApi.getData<MarketingSigninApi.SigninRule>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        bonusEveryDays: data.bonusEveryDays ?? 0,
        bonusPoints: data.bonusPoints ?? 10,
        name: data.name,
        pointsPerDay: data.pointsPerDay,
        remark: data.remark,
        status: data.status ?? 1,
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
