<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderRefundApi } from '#/api/order/refund';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { confirmRefund, getRefundList } from '#/api/order/refund';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Audit from './modules/audit.vue';

const [AuditDrawer, auditDrawerApi] = useVbenDrawer({
  connectedComponent: Audit,
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
          return await getRefundList({
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
            type: formValues?.type,
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
  } as VxeTableGridOptions<OrderRefundApi.Refund>,
});

function onActionClick(e: OnActionClickParams<OrderRefundApi.Refund>) {
  switch (e.code) {
    case 'audit': {
      auditDrawerApi.setData(e.row).open();
      break;
    }
    case 'confirm': {
      onConfirm(e.row);
      break;
    }
  }
}

async function onConfirm(row: OrderRefundApi.Refund) {
  try {
    await ElMessageBox.confirm(
      $t('order.refund.confirmTip', [Number(row.amount).toFixed(2)]),
      $t('order.refund.confirm'),
      { type: 'warning' },
    );
  } catch {
    return;
  }
  await confirmRefund(row.id);
  ElMessage.success($t('order.refund.confirmSuccess'));
  gridApi.query();
}

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <AuditDrawer @success="refreshGrid" />
    <Grid :table-title="$t('order.refund.list')" />
  </Page>
</template>
