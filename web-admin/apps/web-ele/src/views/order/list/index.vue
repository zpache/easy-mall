<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/order/order';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { closeOrder, getOrderList } from '#/api/order/order';
import { $t } from '#/locales';

import Ship from '../delivery/modules/ship.vue';
import { useColumns, useGridFormSchema } from './data';
import Detail from './modules/detail.vue';

const [DetailDrawer, detailDrawerApi] = useVbenDrawer({
  connectedComponent: Detail,
  destroyOnClose: true,
});

const [ShipDrawer, shipDrawerApi] = useVbenDrawer({
  connectedComponent: Ship,
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
            status: formValues?.status,
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
    case 'close': {
      onClose(e.row);
      break;
    }
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

async function onClose(row: OrderApi.Order) {
  try {
    await ElMessageBox.confirm(
      $t('order.order.closeConfirm', [row.orderNo]),
      $t('order.order.close'),
      { type: 'warning' },
    );
  } catch {
    return;
  }
  await closeOrder(row.id);
  ElMessage.success($t('order.order.closeSuccess'));
  gridApi.query();
}

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <DetailDrawer />
    <ShipDrawer @success="refreshGrid" />
    <Grid :table-title="$t('order.order.list')" />
  </Page>
</template>
