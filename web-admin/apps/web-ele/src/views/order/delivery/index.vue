<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/order/order';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElTag } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getOrderList } from '#/api/order/order';
import { $t } from '#/locales';

import Detail from '../list/modules/detail.vue';
import { useColumns, useGridFormSchema } from './data';
import Ship from './modules/ship.vue';

const [ShipDrawer, shipDrawerApi] = useVbenDrawer({
  connectedComponent: Ship,
  destroyOnClose: true,
});

const [DetailDrawer, detailDrawerApi] = useVbenDrawer({
  connectedComponent: Detail,
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
          const range = formValues?.dateRange;
          return await getOrderList({
            endDate: Array.isArray(range) ? range[1] : undefined,
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            refundStatus: formValues?.refundStatus,
            startDate: Array.isArray(range) ? range[0] : undefined,
            status: 1,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<OrderApi.Order>,
});

function onActionClick(e: OnActionClickParams<OrderApi.Order>) {
  switch (e.code) {
    case 'deliver': {
      shipDrawerApi.setData(e.row).open();
      break;
    }
    case 'detail': {
      detailDrawerApi.setData(e.row).open();
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
    <ShipDrawer @success="refreshGrid" />
    <DetailDrawer />
    <Grid :table-title="$t('order.delivery.list')">
      <template #toolbar-tools>
        <ElTag type="warning" effect="plain">
          {{ $t('order.delivery.tip') }}
        </ElTag>
      </template>
    </Grid>
  </Page>
</template>
