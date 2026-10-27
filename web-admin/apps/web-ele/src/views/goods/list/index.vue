<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { GoodsApi } from '#/api/goods/goods';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeGoodsStatus,
  deleteGoods,
  getGoodsList,
} from '#/api/goods/goods';
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
          return await getGoodsList({
            brandId: formValues?.brandId,
            categoryId: formValues?.categoryId,
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
  } as VxeTableGridOptions<GoodsApi.Goods>,
});

function onActionClick(e: OnActionClickParams<GoodsApi.Goods>) {
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

async function onStatusChange(newStatus: number, row: GoodsApi.Goods) {
  const action = newStatus === 1 ? '上架' : '下架';
  try {
    await ElMessageBox.confirm(
      `确定要${action}商品「${row.name}」吗？`,
      `${action}确认`,
      { type: 'warning' },
    );
    await changeGoodsStatus(row.id, newStatus);
    ElMessage.success(`${action}成功`);
    return true;
  } catch {
    return false;
  }
}

async function onDelete(row: GoodsApi.Goods) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('goods.goods.entity')]),
    { type: 'warning' },
  );
  await deleteGoods(row.id);
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
    <Grid :table-title="$t('goods.goods.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('goods.goods.entity')]) }}
        </ElButton>
      </template>
      <template #stock="{ row }">
        <span
          :class="
            row.stock <= (row.stockWarn ?? 0)
              ? 'font-medium text-red-500'
              : ''
          "
        >
          {{ row.stock }} {{ row.unit }}
        </span>
      </template>
    </Grid>
  </Page>
</template>
