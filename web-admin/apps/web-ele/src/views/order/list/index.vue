<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/order/order';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ref } from 'vue';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  closeOrder,
  downloadBlob,
  exportOrders,
  getOrderList,
} from '#/api/order/order';
import { ORDER_PERMISSIONS } from '#/constants/permissions';
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

/** 最近一次列表查询的筛选条件，导出沿用 */
const lastQueryParams = ref<Record<string, any>>({});
const exporting = ref(false);

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
          const params = {
            endDate: Array.isArray(range) ? range[1] : undefined,
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            refundStatus: formValues?.refundStatus,
            startDate: Array.isArray(range) ? range[0] : undefined,
            status: formValues?.status,
          };
          lastQueryParams.value = params;
          return await getOrderList(params);
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

/** 按当前筛选条件导出订单（后端上限 5000 行） */
async function onExport() {
  exporting.value = true;
  try {
    const { startDate, endDate, keyword, refundStatus, status } =
      lastQueryParams.value;
    const blob = await exportOrders({
      endDate,
      keyword,
      refundStatus,
      startDate,
      status,
    });
    downloadBlob(
      blob,
      `订单导出_${new Date().toISOString().slice(0, 16).replaceAll(/[-:T]/g, '')}.xlsx`,
    );
    ElMessage.success($t('order.order.exportSuccess'));
  } finally {
    exporting.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <DetailDrawer />
    <ShipDrawer @success="refreshGrid" />
    <Grid :table-title="$t('order.order.list')">
      <template #toolbar-tools>
        <ElButton
          v-access:code="ORDER_PERMISSIONS.export"
          v-loading="exporting"
          class="mr-2"
          type="primary"
          plain
          @click="onExport"
        >
          {{ $t('order.order.exportBtn') }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
