<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { SystemUserApi } from '#/api/system/user';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElMessage,
  ElMessageBox,
  ElTree,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDeptList } from '#/api/system/dept';
import {
  changeUserStatus,
  deleteUser,
  getUserList,
  resetUserPassword,
} from '#/api/system/user';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

interface DeptNode {
  children?: DeptNode[];
  id: number;
  name: string;
}

const deptTree = ref<DeptNode[]>([]);
const selectedDeptId = ref<number>();

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
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
          return await getUserList({
            deptId: selectedDeptId.value,
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
  } as VxeTableGridOptions<SystemUserApi.SystemUser>,
});

onMounted(async () => {
  deptTree.value = await getDeptList();
});

function onDeptClick(node: DeptNode) {
  selectedDeptId.value = node.id;
  gridApi.query();
}

function resetDeptFilter() {
  selectedDeptId.value = undefined;
  gridApi.query();
}

function onActionClick(e: OnActionClickParams<SystemUserApi.SystemUser>) {
  switch (e.code) {
    case 'delete': {
      onDelete(e.row);
      break;
    }
    case 'edit': {
      formDrawerApi.setData(e.row).open();
      break;
    }
    case 'resetPassword': {
      onResetPassword(e.row);
      break;
    }
  }
}

async function onDelete(row: SystemUserApi.SystemUser) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.nickname || row.username]),
    $t('ui.actionTitle.delete', [$t('system.user.name')]),
    { type: 'warning' },
  );
  await deleteUser(row.id);
  ElMessage.success('删除成功');
  gridApi.query();
}

async function onResetPassword(row: SystemUserApi.SystemUser) {
  const { value } = await ElMessageBox.prompt(
    `为「${row.nickname || row.username}」设置新密码（至少 6 位）`,
    '重置密码',
    {
      inputPattern: /^.{6,32}$/,
      inputErrorMessage: '密码长度需在 6-32 位之间',
      inputType: 'password',
    },
  );
  await resetUserPassword(row.id, value);
  ElMessage.success('密码已重置');
}

async function onStatusChange(
  newStatus: number,
  row: SystemUserApi.SystemUser,
) {
  try {
    await ElMessageBox.confirm(
      `确认${newStatus === 1 ? '启用' : '停用'}用户「${row.nickname || row.username}」吗？`,
      '切换状态',
      { type: 'warning' },
    );
    await changeUserStatus(row.id, newStatus);
    ElMessage.success('状态已更新');
    return true;
  } catch {
    return false;
  }
}

function onCreate() {
  formDrawerApi.setData({}).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <div class="flex h-full gap-3">
      <div class="bg-card flex w-[240px] shrink-0 flex-col rounded-lg border p-3">
        <div class="mb-2 flex items-center justify-between">
          <span class="text-sm font-medium">{{ $t('system.user.dept') }}</span>
          <ElButton link size="small" type="primary" @click="resetDeptFilter">
            全部
          </ElButton>
        </div>
        <ElTree
          :data="deptTree"
          :expand-on-click-node="false"
          :props="{ children: 'children', label: 'name' }"
          class="flex-1 overflow-auto"
          default-expand-all
          highlight-current
          node-key="id"
          @node-click="onDeptClick"
        />
      </div>
      <div class="min-w-0 flex-1">
        <Grid :table-title="$t('system.user.list')">
          <template #toolbar-tools>
            <ElButton type="primary" @click="onCreate">
              <Plus class="size-5" />
              {{ $t('ui.actionTitle.create', [$t('system.user.name')]) }}
            </ElButton>
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>
