<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MarketingReductionApi } from '#/api/marketing/reduction';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeReductionStatus,
  deleteReduction,
  getReductionList,
} from '#/api/marketing/reduction';
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
          return await getReductionList({
            keyword: formValues?.keyword,
            mode: formValues?.mode,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
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
  } as VxeTableGridOptions<MarketingReductionApi.Reduction>,
});

function onActionClick(e: OnActionClickParams<MarketingReductionApi.Reduction>) {
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
  row: MarketingReductionApi.Reduction,
) {
  await changeReductionStatus(row.id, newStatus);
  ElMessage.success($t('marketing.reduction.statusUpdated'));
  return true;
}

async function onDelete(row: MarketingReductionApi.Reduction) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('marketing.reduction.entity')]),
    { type: 'warning' },
  );
  await deleteReduction(row.id);
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
    <Grid :table-title="$t('marketing.reduction.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('marketing.reduction.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
