<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { GoodsBrandApi } from '#/api/goods/brand';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeBrandStatus,
  deleteBrand,
  getBrandList,
} from '#/api/goods/brand';
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
          return await getBrandList({
            keyword: formValues?.keyword,
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
  } as VxeTableGridOptions<GoodsBrandApi.Brand>,
});

function onActionClick(e: OnActionClickParams<GoodsBrandApi.Brand>) {
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

async function onStatusChange(newStatus: number, row: GoodsBrandApi.Brand) {
  await changeBrandStatus(row.id, newStatus);
  ElMessage.success('状态已更新');
  return true;
}

async function onDelete(row: GoodsBrandApi.Brand) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('goods.brand.entity')]),
    { type: 'warning' },
  );
  await deleteBrand(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData(null).open();
}

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refreshGrid" />
    <Grid :table-title="$t('goods.brand.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('goods.brand.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
