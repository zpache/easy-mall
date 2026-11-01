<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemLogApi } from '#/api/system/log';

import { Page } from '@vben/common-ui';

import { ElTag } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getLoginLogPage } from '#/api/system/log';

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        component: 'Input',
        componentProps: { placeholder: '登录账号', allowClear: true },
        fieldName: 'username',
        label: '账号',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: [
            { label: '成功', value: 1 },
            { label: '失败', value: 0 },
          ],
          placeholder: '全部结果',
        },
        fieldName: 'success',
        label: '结果',
      },
    ],
    submitOnChange: true,
  },
  gridOptions: {
    columns: [
      { field: 'id', title: 'ID', width: 80 },
      {
        field: 'createdAt',
        formatter: ({ row }) =>
          row.createdAt ? String(row.createdAt).replace('T', ' ').slice(0, 19) : '',
        title: '时间',
        width: 160,
      },
      { field: 'username', title: '账号', width: 120 },
      {
        field: 'success',
        slots: { default: 'success' },
        title: '结果',
        width: 80,
      },
      { field: 'message', title: '说明', minWidth: 160 },
      { field: 'ip', title: 'IP', width: 130 },
      { field: 'userAgent', showOverflow: true, title: 'User-Agent', minWidth: 240 },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getLoginLogPage({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            success: formValues?.success,
            username: formValues?.username,
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
  } as VxeTableGridOptions<SystemLogApi.LoginLog>,
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="登录日志">
      <template #success="{ row }">
        <ElTag :type="row.success === 1 ? 'success' : 'danger'" effect="plain">
          {{ row.success === 1 ? '成功' : '失败' }}
        </ElTag>
      </template>
    </Grid>
  </Page>
</template>
