<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { GoodsStockApi } from '#/api/goods/stock';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getStockLogs } from '#/api/goods/stock';
import { $t } from '#/locales';

import { useLogColumns } from '../data';

const current = ref<null | GoodsStockApi.Stock>(null);

const getTitle = computed(
  () => `${$t('goods.stock.logs')} · ${current.value?.name ?? ''}`,
);

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useLogColumns(),
    height: 480,
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          if (!current.value) {
            return { items: [], total: 0 };
          }
          return await getStockLogs({
            goodsId: current.value.goodsId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      zoom: false,
    },
  } as VxeTableGridOptions<GoodsStockApi.StockLog>,
});

const [Drawer, drawerApi] = useVbenDrawer({
  footer: false,
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    current.value = drawerApi.getData<GoodsStockApi.Stock>() ?? null;
    gridApi.query();
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[880px]">
    <Grid />
  </Drawer>
</template>
