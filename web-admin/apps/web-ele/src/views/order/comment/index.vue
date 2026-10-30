<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderCommentApi } from '#/api/order/comment';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElAlert, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeCommentVisible,
  deleteComment,
  getCommentList,
  getPendingCommentCount,
} from '#/api/order/comment';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Audit from './modules/audit.vue';
import Reply from './modules/reply.vue';

/** 待审核数量：页面顶部提示运营处理，审核后随列表刷新 */
const pendingCount = ref(0);

const [AuditDrawer, auditDrawerApi] = useVbenDrawer({
  connectedComponent: Audit,
  destroyOnClose: true,
});

const [ReplyDrawer, replyDrawerApi] = useVbenDrawer({
  connectedComponent: Reply,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick, onVisibleChange),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          loadPendingCount();
          return await getCommentList({
            auditStatus: formValues?.auditStatus,
            isShow: formValues?.isShow,
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            score: formValues?.score,
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
  } as VxeTableGridOptions<OrderCommentApi.Comment>,
});

async function loadPendingCount() {
  try {
    pendingCount.value = await getPendingCommentCount();
  } catch {
    pendingCount.value = 0;
  }
}

onMounted(loadPendingCount);

function onActionClick(e: OnActionClickParams<OrderCommentApi.Comment>) {
  switch (e.code) {
    case 'audit': {
      auditDrawerApi.setData(e.row).open();
      break;
    }
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'reply': {
      replyDrawerApi.setData(e.row).open();
      break;
    }
  }
}

async function onVisibleChange(
  newValue: number,
  row: OrderCommentApi.Comment,
) {
  // 未通过审核的评价没有展示资格，先审核再谈显示 / 隐藏
  if (row.auditStatus !== 1) {
    ElMessage.warning($t('order.comment.auditFirst'));
    return false;
  }
  const action = newValue === 1 ? '显示' : '隐藏';
  try {
    await ElMessageBox.confirm(
      `确定要${action}这条评价吗？${action}后将立即对小程序端生效。`,
      `${action}评价确认`,
      { type: 'warning' },
    );
  } catch {
    return false;
  }
  await changeCommentVisible(row.id, newValue);
  ElMessage.success($t('order.comment.visibleSuccess'));
  return true;
}

async function onDelete(row: OrderCommentApi.Comment) {
  try {
    await ElMessageBox.confirm(
      $t('order.comment.deleteConfirm'),
      $t('order.comment.delete'),
      { type: 'warning' },
    );
  } catch {
    return;
  }
  await deleteComment(row.id);
  ElMessage.success($t('order.comment.deleteSuccess'));
  gridApi.query();
}

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <AuditDrawer @success="refreshGrid" />
    <ReplyDrawer @success="refreshGrid" />
    <ElAlert
      v-if="pendingCount > 0"
      class="mb-2"
      :closable="false"
      show-icon
      :title="`${pendingCount} ${$t('order.comment.pendingTip')}`"
      type="warning"
    />
    <Grid :table-title="$t('order.comment.list')" />
  </Page>
</template>
