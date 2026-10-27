<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { GoodsCategoryApi } from '#/api/goods/category';

import { Page, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteCategory, getCategoryTree } from '#/api/goods/category';
import { $t } from '#/locales';

import { useColumns } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getCategoryTree();
          return { items: list, total: list.length };
        },
      },
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      zoom: true,
    },
    treeConfig: {
      parentField: 'parentId',
      rowField: 'id',
      transform: false,
    },
  } as VxeTableGridOptions,
});

function onCreate() {
  formModalApi.setData(null).open();
}

function onAppend(row: GoodsCategoryApi.Category) {
  formModalApi.setData({ parentId: row.id }).open();
}

function onEdit(row: GoodsCategoryApi.Category) {
  formModalApi.setData(row).open();
}

async function onDelete(row: GoodsCategoryApi.Category) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('goods.category.entity')]),
    { type: 'warning' },
  );
  await deleteCategory(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  refreshGrid();
}

function onActionClick({
  code,
  row,
}: OnActionClickParams<GoodsCategoryApi.Category>) {
  switch (code) {
    case 'append': {
      onAppend(row);
      break;
    }
    case 'delete': {
      onDelete(row).catch(() => {});
      break;
    }
    case 'edit': {
      onEdit(row);
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
    <FormModal @success="refreshGrid" />
    <Grid :table-title="$t('goods.category.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('goods.category.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
