<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { GoodsStockApi } from '#/api/goods/stock';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElTag } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getStockList } from '#/api/goods/stock';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Adjust from './modules/adjust.vue';
import Logs from './modules/logs.vue';

const [AdjustDrawer, adjustDrawerApi] = useVbenDrawer({
  connectedComponent: Adjust,
  destroyOnClose: true,
});

const [LogsDrawer, logsDrawerApi] = useVbenDrawer({
  connectedComponent: Logs,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getStockList({
            keyword: formValues?.keyword,
            lowStock: formValues?.lowStock === true ? true : undefined,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
          });
        },
      },
    },
    rowConfig: { keyField: 'goodsId' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<GoodsStockApi.Stock>,
});

function onActionClick(e: OnActionClickParams<GoodsStockApi.Stock>) {
  switch (e.code) {
    case 'adjust': {
      adjustDrawerApi.setData(e.row).open();
      break;
    }
    case 'logs': {
      logsDrawerApi.setData(e.row).open();
      break;
    }
  }
}

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <AdjustDrawer @success="refreshGrid" />
    <LogsDrawer />
    <Grid :table-title="$t('goods.stock.list')">
      <template #toolbar-tools>
        <ElTag type="warning" effect="plain">
          {{ $t('goods.stock.lowStockTip') }}
        </ElTag>
      </template>
    </Grid>
  </Page>
</template>
