<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MarketingSigninApi } from '#/api/marketing/signin';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeSigninRuleStatus,
  deleteSigninRule,
  getSigninRuleList,
} from '#/api/marketing/signin';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Logs from './modules/logs.vue';
import Form from './modules/rule-form.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [LogsDrawer, logsDrawerApi] = useVbenDrawer({
  connectedComponent: Logs,
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
          return await getSigninRuleList({
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
  } as VxeTableGridOptions<MarketingSigninApi.SigninRule>,
});

function onActionClick(e: OnActionClickParams<MarketingSigninApi.SigninRule>) {
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

async function onStatusChange(
  newStatus: number,
  row: MarketingSigninApi.SigninRule,
) {
  await changeSigninRuleStatus(row.id, newStatus);
  ElMessage.success($t('marketing.signin.statusUpdated'));
  return true;
}

async function onDelete(row: MarketingSigninApi.SigninRule) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('marketing.signin.entity')]),
    { type: 'warning' },
  );
  await deleteSigninRule(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData(null).open();
}

function onLogs() {
  logsDrawerApi.open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <LogsDrawer />
    <Grid :table-title="$t('marketing.signin.rulesList')">
      <template #toolbar-tools>
        <ElButton class="mr-2" @click="onLogs">
          {{ $t('marketing.signin.logs') }}
        </ElButton>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('marketing.signin.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
