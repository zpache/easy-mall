<script lang="ts" setup>
import type {
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MemberPointsApi } from '#/api/member/points';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getPointsLogs } from '#/api/member/points';
import { $t } from '#/locales';

import AdjustDrawer from '../list/modules/points.vue';
import { useColumns, useGridFormSchema } from './data';

const [PointAdjustDrawer, adjustDrawerApi] = useVbenDrawer({
  connectedComponent: AdjustDrawer,
  destroyOnClose: true,
});

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
          return await getPointsLogs({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            type: formValues?.type,
            userId: formValues?.userId,
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
  } as VxeTableGridOptions<MemberPointsApi.PointsLog>,
});

function onAdjust() {
  adjustDrawerApi.open();
}
</script>

<template>
  <Page auto-content-height>
    <PointAdjustDrawer @success="gridApi.query()" />
    <Grid :table-title="$t('member.points.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onAdjust">
          <Plus class="size-5" />
          {{ $t('member.points.adjustAny') }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
