<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MarketingCouponApi } from '#/api/marketing/coupon';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeCouponStatus,
  deleteCoupon,
  getCouponList,
} from '#/api/marketing/coupon';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import Records from './modules/records.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [RecordsDrawer, recordsDrawerApi] = useVbenDrawer({
  connectedComponent: Records,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick, onStatusChange),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getCouponList({
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
  } as VxeTableGridOptions<MarketingCouponApi.Coupon>,
});

function onActionClick(e: OnActionClickParams<MarketingCouponApi.Coupon>) {
  switch (e.code) {
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
  }
}

async function onStatusChange(newStatus: number, row: MarketingCouponApi.Coupon) {
  await changeCouponStatus(row.id, newStatus);
  ElMessage.success($t('marketing.coupon.statusUpdated'));
  return true;
}

async function onDelete(row: MarketingCouponApi.Coupon) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('marketing.coupon.entity')]),
    { type: 'warning' },
  );
  await deleteCoupon(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData(null).open();
}

function onRecords() {
  recordsDrawerApi.open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <RecordsDrawer />
    <Grid :table-title="$t('marketing.coupon.list')">
      <template #toolbar-tools>
        <ElButton class="mr-2" @click="onRecords">
          {{ $t('marketing.coupon.records') }}
        </ElButton>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('marketing.coupon.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
