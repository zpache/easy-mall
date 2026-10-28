<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { DecorateApi } from '#/api/decorate/page';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  copyDecoratePage,
  createDecoratePage,
  deleteDecoratePage,
  getDecoratePages,
} from '#/api/decorate/page';
import { $t } from '#/locales';

import PreviewQrcode from '../editor/components/preview-qrcode.vue';
import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const router = useRouter();

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
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
          return await getDecoratePages({
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
  } as VxeTableGridOptions<DecorateApi.Page>,
});

const previewVisible = ref(false);
const previewPage = ref<DecorateApi.Page>();

function onActionClick(e: OnActionClickParams<DecorateApi.Page>) {
  const row = e.row;
  switch (e.code) {
    case 'copy': {
      void onCopy(row);
      break;
    }
    case 'delete': {
      void onDelete(row);
      break;
    }
    case 'edit': {
      router.push(`/decorate/editor/${row.id}`);
      break;
    }
    case 'preview': {
      previewPage.value = row;
      previewVisible.value = true;
      break;
    }
  }
}

async function onCopy(row: DecorateApi.Page) {
  const copy = await copyDecoratePage(row.id);
  ElMessage.success(`已复制为「${copy.name}」`);
  gridApi.query();
}

async function onDelete(row: DecorateApi.Page) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', ['页面']),
    { type: 'warning' },
  );
  await deleteDecoratePage(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  gridApi.query();
}

async function onCreate(values: DecorateApi.PagePayload) {
  const page = await createDecoratePage(values);
  ElMessage.success('页面已创建，正在打开编辑器');
  formModalApi.close();
  router.push(`/decorate/editor/${page.id}`);
}
</script>

<template>
  <Page auto-content-height>
    <FormModal @submit="onCreate" />
    <PreviewQrcode
      v-model:visible="previewVisible"
      :page-id="previewPage?.id"
      :page-name="previewPage?.name"
      :preview-code="previewPage?.previewCode"
    />
    <Grid table-title="装修页面">
      <template #toolbar-tools>
        <ElButton type="primary" @click="formModalApi.setData(null).open()">
          <Plus class="size-5" />
          新建页面
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
