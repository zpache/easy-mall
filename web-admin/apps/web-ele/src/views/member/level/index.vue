<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MemberLevelApi } from '#/api/member/level';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { onMounted, ref } from 'vue';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElMessage,
  ElMessageBox,
  ElSwitch,
  ElTag,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeLevelStatus,
  deleteLevel,
  getLevelDiscountSwitch,
  getLevelList,
  updateLevelDiscountSwitch,
} from '#/api/member/level';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

/** 等级折扣总开关：全局唯一，关闭后所有等级折扣停止参与下单计价 */
const discountEnabled = ref(true);
const discountLevelCount = ref(0);
const switchLoading = ref(false);
const switchSaving = ref(false);

async function loadDiscountSwitch() {
  switchLoading.value = true;
  try {
    const data = await getLevelDiscountSwitch();
    discountEnabled.value = data.enabled;
    discountLevelCount.value = data.discountLevelCount ?? 0;
  } catch {
    // 拉取失败不阻塞等级列表，开关按默认开启展示
  } finally {
    switchLoading.value = false;
  }
}

async function onDiscountSwitchChange(value: boolean | number | string) {
  const enabled = Boolean(value);
  const action = enabled ? '开启' : '关闭';
  try {
    await ElMessageBox.confirm(
      enabled
        ? '开启后，会员下单会按等级折扣率优惠商品金额（只作用于商品金额，不抵运费，可与优惠券叠加）。'
        : `关闭后，当前 ${discountLevelCount.value} 个等级的折扣将全部停止生效，订单按商品原价结算；等级本身与升级门槛不受影响。`,
      `${action}等级折扣`,
      { type: enabled ? 'info' : 'warning' },
    );
  } catch {
    return;
  }
  switchSaving.value = true;
  try {
    const data = await updateLevelDiscountSwitch(enabled);
    discountEnabled.value = data.enabled;
    discountLevelCount.value = data.discountLevelCount ?? 0;
    ElMessage.success(`等级折扣已${action}，约 30 秒内对新下单生效`);
  } finally {
    switchSaving.value = false;
  }
}

onMounted(loadDiscountSwitch);

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
          return await getLevelList({
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
  } as VxeTableGridOptions<MemberLevelApi.Level>,
});

function onActionClick(e: OnActionClickParams<MemberLevelApi.Level>) {
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

async function onStatusChange(newStatus: number, row: MemberLevelApi.Level) {
  const action = newStatus === 1 ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(
      `确定要${action}会员等级「${row.name}」吗？`,
      `${action}确认`,
      { type: 'warning' },
    );
  } catch {
    return false;
  }
  await changeLevelStatus(row.id, newStatus);
  ElMessage.success($t('member.level.statusUpdated'));
  return true;
}

async function onDelete(row: MemberLevelApi.Level) {
  await ElMessageBox.confirm(
    $t('ui.actionMessage.deleteConfirm', [row.name]),
    $t('ui.actionTitle.delete', [$t('member.level.entity')]),
    { type: 'warning' },
  );
  await deleteLevel(row.id);
  ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData(null).open();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <ElCard shadow="never" class="mb-3">
      <div class="discount-switch">
        <div class="discount-switch__main">
          <div class="discount-switch__title">
            <span>等级折扣总开关</span>
            <ElTag
              :type="discountEnabled ? 'success' : 'info'"
              effect="light"
              size="small"
            >
              {{ discountEnabled ? '已开启' : '已关闭' }}
            </ElTag>
          </div>
          <div class="discount-switch__tip">
            开启后，会员下单按等级折扣率优惠商品金额（只作用于商品金额，不抵运费，可与优惠券叠加）；
            关闭后所有等级折扣停止生效，订单按原价结算，等级与升级门槛保持不变。
            开关改动约 30 秒内同步到小程序下单。
          </div>
        </div>
        <div class="discount-switch__action">
          <span class="discount-switch__count">
            当前 {{ discountLevelCount }} 个等级享受折扣
          </span>
          <ElSwitch
            :loading="switchLoading || switchSaving"
            :model-value="discountEnabled"
            @change="onDiscountSwitchChange"
          />
        </div>
      </div>
      <ElAlert
        v-if="!discountEnabled"
        :closable="false"
        class="mt-3"
        show-icon
        title="总开关已关闭：下方折扣率仅作为等级权益配置保留，下单不参与计价"
        type="warning"
      />
    </ElCard>
    <Grid :table-title="$t('member.level.list')">
      <template #toolbar-tools>
        <ElButton type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('ui.actionTitle.create', [$t('member.level.entity')]) }}
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.discount-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.discount-switch__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.discount-switch__tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.discount-switch__action {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
}

.discount-switch__count {
  font-size: 12px;
  color: #909399;
}
</style>
