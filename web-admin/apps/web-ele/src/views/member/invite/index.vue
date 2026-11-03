<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { Page } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getInviteRecords } from '#/api/member/invite';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';

defineOptions({ name: 'MemberInvite' });

const [Grid] = useVbenVxeGrid({
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
          return await getInviteRecords({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
            keyword: formValues?.keyword,
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
</script>

<template>
  <Page auto-content-height>
    <Grid :table-title="$t('member.invite.list')" />
  </Page>
</template>
