<script lang="ts" setup>
import type { GoodsStockApi } from '#/api/goods/stock';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage, ElOption, ElSelect } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { getGoodsSku } from '#/api/goods/goods';
import { adjustStock } from '#/api/goods/stock';
import { $t } from '#/locales';

import { useAdjustSchema } from '../data';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | GoodsStockApi.Stock>(null);

/** 多规格商品的可选 SKU：有值时必须以 SKU 维度调整，避免与 SKU 库存脱节 */
const skuOptions = ref<Array<{ id: number; specText: string; stock: number }>>(
  [],
);
const selectedSkuId = ref<number | undefined>(undefined);

const getTitle = computed(() => {
  const goods = current.value;
  const label = goods?.goodsCode ? `${goods.goodsCode} · ${goods.name}` : (goods?.name ?? '');
  return `${$t('goods.stock.adjust')} · ${label}`;
});

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
    if (skuOptions.value.length > 0 && !selectedSkuId.value) {
      ElMessage.warning('该商品为多规格商品，请先选择要调整的规格');
      return;
    }
    drawerApi.lock();
    try {
      await adjustStock({
        goodsId: current.value.goodsId,
        qty: Number(values.qty),
        remark: values.remark,
        skuId: selectedSkuId.value,
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
    skuOptions.value = [];
    selectedSkuId.value = undefined;
    if (data) {
      // 多规格商品：拉取启用中的 SKU，调整时按 SKU 落库存与流水
      getGoodsSku(data.goodsId)
        .then((config) => {
          skuOptions.value = (config.skus ?? [])
            .filter((sku) => (sku.status ?? 1) === 1)
            .map((sku) => ({
              id: Number(sku.id),
              specText: sku.specText,
              stock: Number(sku.stock ?? 0),
            }));
        })
        .catch(() => {
          skuOptions.value = [];
        });
      formApi.setValues({
        goodsName: `${data.goodsCode ? `${data.goodsCode} · ` : ''}${data.name}（${$t('goods.stock.stock')}：${data.stock}）`,
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
    <div v-if="skuOptions.length > 0" class="mb-4">
      <div class="mb-2 text-sm text-gray-500">选择规格（多规格商品按 SKU 调整）</div>
      <ElSelect v-model="selectedSkuId" class="w-full" placeholder="请选择规格">
        <ElOption
          v-for="sku in skuOptions"
          :key="sku.id"
          :label="`${sku.specText}（库存 ${sku.stock}）`"
          :value="sku.id"
        />
      </ElSelect>
    </div>
    <Form />
  </Drawer>
</template>
