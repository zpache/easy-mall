<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MarketingPromotionApi } from '#/api/marketing/promotion';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changePromotionStatus,
  deletePromotion,
  getPromotionList,
} from '#/api/marketing/promotion';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
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
          return await getPromotionList({
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
  } as VxeTableGridOptions<MarketingPromotionApi.Promotion>,
});

function onActionClick(e: OnActionClickParams<MarketingPromotionApi.Promotion>) {
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

async function onStatusChange(
  newStatus: number,
  row: MarketingPromotionApi.Promotion,
) {
  await changePromotionStatus(row.id, newStatus);
  ElMessage.success($t('marketing.promotion.statusUpdated'));
  return true;
}

async function onDelete(row: MarketingPromotionApi.Promotion) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('marketing.promotion.entity')]),
    { type: 'warning' },
  );
  await deletePromotion(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData(null).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <Grid :table-title="$t('marketing.promotion.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('marketing.promotion.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
