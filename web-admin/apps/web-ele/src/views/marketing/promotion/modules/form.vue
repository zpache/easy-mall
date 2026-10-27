<script lang="ts" setup>
import type { MarketingPromotionApi } from '#/api/marketing/promotion';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createPromotion, updatePromotion } from '#/api/marketing/promotion';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MarketingPromotionApi.Promotion>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('marketing.promotion.edit')
    : $t('marketing.promotion.create'),
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
    const payload: MarketingPromotionApi.PromotionPayload = {
      endTime: values.endTime,
      goodsId: Number(values.goodsId),
      goodsName: values.goodsName,
      groupHours: values.type === 2 ? Number(values.groupHours) : undefined,
      groupSize: values.type === 2 ? Number(values.groupSize) : undefined,
      name: values.name,
      originalPrice: Number(values.originalPrice),
      promoPrice: Number(values.promoPrice),
      startTime: values.startTime,
      status: values.status ?? 1,
      stock: Number(values.stock),
      type: Number(values.type),
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updatePromotion(formData.value.id, payload)
        : createPromotion(payload));
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
    const data = drawerApi.getData<MarketingPromotionApi.Promotion>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        endTime: data.endTime,
        goodsId: data.goodsId,
        goodsName: data.goodsName,
        groupHours: data.groupHours ?? 24,
        groupSize: data.groupSize ?? 3,
        name: data.name,
        originalPrice: Number(data.originalPrice),
        promoPrice: Number(data.promoPrice),
        startTime: data.startTime,
        status: data.status ?? 1,
        stock: data.stock,
        type: data.type,
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
