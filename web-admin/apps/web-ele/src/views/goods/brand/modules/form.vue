<script lang="ts" setup>
import type { GoodsBrandApi } from '#/api/goods/brand';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createBrand, updateBrand } from '#/api/goods/brand';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | GoodsBrandApi.Brand>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('goods.brand.entity')])
    : $t('ui.actionTitle.create', [$t('goods.brand.entity')]),
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
    const payload: GoodsBrandApi.BrandPayload = {
      logo: values.logo,
      name: values.name,
      remark: values.remark,
      sort: values.sort,
      status: values.status ?? 1,
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateBrand(formData.value.id, payload)
        : createBrand(payload));
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
    const data = drawerApi.getData<GoodsBrandApi.Brand>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        logo: data.logo,
        name: data.name,
        remark: data.remark,
        sort: data.sort ?? 0,
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
