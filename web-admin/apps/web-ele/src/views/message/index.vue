<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { Page } from '@vben/common-ui';

import { ElButton, ElMessage } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMessages, readAllMessages } from '#/api/message';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MessageCenter' });

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getMessages({
            onlyUnread: formValues?.onlyUnread,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
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
  } as VxeTableGridOptions,
});

/** 全部已读是全局操作：一条消息只能被处理一次 */
async function handleReadAll() {
  await readAllMessages();
  ElMessage.success($t('message.readAllSuccess'));
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <Grid :table-title="$t('message.list')">
      <template #toolbar-tools>
        <ElButton class="mr-2" plain type="primary" @click="handleReadAll">
          {{ $t('message.readAll') }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
