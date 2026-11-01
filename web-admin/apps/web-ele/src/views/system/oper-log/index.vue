<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemLogApi } from '#/api/system/log';

import { Page } from '@vben/common-ui';

import { ElTag } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getOperLogPage } from '#/api/system/log';

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        component: 'Input',
        componentProps: { placeholder: '操作人账号', allowClear: true },
        fieldName: 'username',
        label: '操作人',
      },
      {
        component: 'Input',
        componentProps: { placeholder: '业务模块，如 订单-订单', allowClear: true },
        fieldName: 'module',
        label: '模块',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: [
            { label: '成功', value: 0 },
            { label: '失败', value: 500 },
          ],
          placeholder: '全部结果',
        },
        fieldName: 'resultCode',
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
      { field: 'username', title: '操作人', width: 100 },
      { field: 'module', title: '模块', minWidth: 140 },
      { field: 'action', title: '操作', minWidth: 140 },
      { field: 'method', title: '方式', width: 70 },
      { field: 'uri', title: '接口', minWidth: 180 },
      {
        field: 'params',
        formatter: ({ row }) => row.params ?? '',
        showOverflow: true,
        title: '参数',
        minWidth: 200,
      },
      {
        field: 'resultCode',
        slots: { default: 'resultCode' },
        title: '结果',
        width: 80,
      },
      { field: 'errorMsg', showOverflow: true, title: '失败原因', minWidth: 160 },
      { field: 'ip', title: 'IP', width: 130 },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getOperLogPage({
            module: formValues?.module,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            resultCode: formValues?.resultCode,
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
  } as VxeTableGridOptions<SystemLogApi.OperLog>,
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="操作日志">
      <template #resultCode="{ row }">
        <ElTag :type="row.resultCode === 0 ? 'success' : 'danger'" effect="plain">
          {{ row.resultCode === 0 ? '成功' : '失败' }}
        </ElTag>
      </template>
    </Grid>
  </Page>
</template>
