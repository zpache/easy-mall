<script lang="ts" setup>
import type {
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSigninLogs } from '#/api/marketing/signin';
import { $t } from '#/locales';

import { useLogColumns } from '../data';

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        component: 'InputNumber',
        componentProps: {
          class: 'w-full',
          controlsPosition: 'right',
          min: 1,
          placeholder: $t('marketing.signin.userIdPlaceholder'),
        },
        fieldName: 'userId',
        label: $t('marketing.signin.logUser'),
      },
      {
        component: 'DatePicker',
        componentProps: {
          class: 'w-full',
          placeholder: $t('marketing.signin.beginDate'),
          type: 'date',
          valueFormat: 'YYYY-MM-DD',
        },
        fieldName: 'beginDate',
        label: $t('marketing.signin.beginDate'),
      },
      {
        component: 'DatePicker',
        componentProps: {
          class: 'w-full',
          placeholder: $t('marketing.signin.endDate'),
          type: 'date',
          valueFormat: 'YYYY-MM-DD',
        },
        fieldName: 'endDate',
        label: $t('marketing.signin.endDate'),
      },
    ],
    submitOnChange: true,
  },
  gridOptions: {
    columns: useLogColumns(),
    height: 440,
    keepSource: true,
    pagerConfig: {
      pageSize: 10,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getSigninLogs({
            beginDate: formValues?.beginDate,
            endDate: formValues?.endDate,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            userId: formValues?.userId,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: false,
      export: false,
      refresh: true,
      search: true,
      zoom: false,
    },
  } as VxeTableGridOptions,
});

const [Drawer] = useVbenDrawer({
  footer: false,
});
</script>

<template>
  <Drawer :title="$t('marketing.signin.logsList')" class="w-[860px]">
    <Grid />
  </Drawer>
</template>
