<script lang="ts" setup>
import type { GoodsStockApi } from '#/api/goods/stock';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { adjustStock } from '#/api/goods/stock';
import { $t } from '#/locales';

import { useAdjustSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | GoodsStockApi.Stock>(null);

const getTitle = computed(
  () => `${$t('goods.stock.adjust')} · ${current.value?.name ?? ''}`,
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useAdjustSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    if (!current.value) {
      return;
    }
    drawerApi.lock();
    try {
      await adjustStock({
        goodsId: current.value.goodsId,
        qty: Number(values.qty),
        remark: values.remark,
        type: values.type,
      });
      ElMessage.success('库存调整成功');
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
    const data = drawerApi.getData<GoodsStockApi.Stock>();
    current.value = data && data.goodsId ? data : null;
    formApi.resetForm();
    if (data) {
      formApi.setValues({
        goodsName: `${data.name}（${$t('goods.stock.stock')}：${data.stock}）`,
        qty: undefined,
        remark: undefined,
        type: 'in',
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[460px]">
    <Form />
  </Drawer>
</template>
