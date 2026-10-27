<script lang="ts" setup>
import type { GoodsApi } from '#/api/goods/goods';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createGoods, updateGoods } from '#/api/goods/goods';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | GoodsApi.Goods>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('goods.goods.entity')])
    : $t('ui.actionTitle.create', [$t('goods.goods.entity')]),
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
    const payload: GoodsApi.GoodsPayload = {
      brandId: values.brandId,
      categoryId: Number(values.categoryId),
      detail: values.detail,
      images: values.images,
      mainImage: values.mainImage,
      name: values.name,
      originalPrice: values.originalPrice,
      price: Number(values.price),
      sort: values.sort,
      status: values.status ?? 1,
      stock: Number(values.stock),
      stockWarn: values.stockWarn,
      subtitle: values.subtitle,
      unit: values.unit,
      version: formData.value?.version,
    };
    try {
      await (formData.value?.id
        ? updateGoods(formData.value.id, payload)
        : createGoods(payload));
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
    const data = drawerApi.getData<GoodsApi.Goods>();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        brandId: data.brandId,
        categoryId: data.categoryId,
        detail: data.detail,
        images: data.images,
        mainImage: data.mainImage,
        name: data.name,
        originalPrice: data.originalPrice,
        price: data.price,
        sort: data.sort ?? 0,
        status: data.status ?? 1,
        stock: data.stock,
        stockWarn: data.stockWarn ?? 10,
        subtitle: data.subtitle,
        unit: data.unit ?? '件',
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[620px]">
    <Form />
  </Drawer>
</template>
