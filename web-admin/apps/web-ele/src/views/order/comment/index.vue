<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderCommentApi } from '#/api/order/comment';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeCommentVisible,
  deleteComment,
  getCommentList,
} from '#/api/order/comment';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Reply from './modules/reply.vue';

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
          return await getCommentList({
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

function onActionClick(e: OnActionClickParams<OrderCommentApi.Comment>) {
  switch (e.code) {
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
    <ReplyDrawer @success="refreshGrid" />
    <Grid :table-title="$t('order.comment.list')" />
  </Page>
</template>
