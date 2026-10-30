<script lang="ts" setup>
import type { GoodsCategoryApi } from '#/api/goods/category';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createCategory, updateCategory } from '#/api/goods/category';
import { $t } from '#/locales';

import { useSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | GoodsCategoryApi.Category>(null);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', [$t('goods.category.entity')])
    : $t('ui.actionTitle.create', [$t('goods.category.entity')]),
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      const payload: GoodsCategoryApi.CategoryPayload = {
        name: values.name,
        // 不选上级表示一级分类，后端用 0 表示根
        parentId: values.parentId ?? 0,
        sort: values.sort,
        status: values.status ?? 1,
        version: formData.value?.version,
      };
      await (formData.value?.id
        ? updateCategory(formData.value.id, payload)
        : createCategory(payload));
      emit('success');
      modalApi.close();
    } finally {
      modalApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerData();
    formData.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        name: data.name,
        parentId: data.parentId === 0 ? undefined : data.parentId,
        sort: data.sort ?? 0,
        status: data.status ?? 1,
      });
    } else if (data?.parentId) {
      formApi.setValues({ parentId: data.parentId, sort: 0, status: 1 });
    }
  },
});

function drawerData() {
  return modalApi.getData<GoodsCategoryApi.Category>();
}
</script>

<template>
  <Modal :title="getTitle">
    <Form />
  </Modal>
</template>
