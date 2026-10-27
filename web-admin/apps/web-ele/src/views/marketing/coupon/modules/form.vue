<script lang="ts" setup>
import type { MarketingCouponApi } from '#/api/marketing/coupon';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createCoupon, updateCoupon } from '#/api/marketing/coupon';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | MarketingCouponApi.Coupon>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('marketing.coupon.edit')
    : $t('marketing.coupon.create'),
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
    const payload: MarketingCouponApi.CouponPayload = {
      amount: values.type !== 2 ? Number(values.amount) : undefined,
      discount: values.type === 2 ? Number(values.discount) : undefined,
      name: values.name,
      perLimit: Number(values.perLimit ?? 1),
      remark: values.remark,
      status: values.status ?? 1,
      threshold: Number(values.threshold ?? 0),
      totalCount: Number(values.totalCount),
      type: Number(values.type),
      validDays: values.validType === 2 ? Number(values.validDays) : undefined,
      validEnd: values.validType === 1 ? values.validRange?.[1] : undefined,
      validStart: values.validType === 1 ? values.validRange?.[0] : undefined,
      validType: Number(values.validType),
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateCoupon(formData.value.id, payload)
        : createCoupon(payload));
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
    const data = drawerApi.getData<MarketingCouponApi.Coupon>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        amount: data.type !== 2 ? Number(data.amount) : undefined,
        discount: data.type === 2 ? Number(data.discount) : undefined,
        name: data.name,
        perLimit: data.perLimit ?? 1,
        remark: data.remark,
        status: data.status ?? 1,
        threshold: Number(data.threshold ?? 0),
        totalCount: data.totalCount,
        type: data.type,
        validDays: data.validDays ?? 7,
        validRange:
          data.validType === 1 && data.validStart && data.validEnd
            ? [data.validStart, data.validEnd]
            : undefined,
        validType: data.validType ?? 1,
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
