<script lang="ts" setup>
import type {
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MemberPointsApi } from '#/api/member/points';

import { reactive, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElSwitch,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getPointsLogs,
  getPointsRule,
  updatePointsRule,
} from '#/api/member/points';
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

/* ---------------- 积分规则 ---------------- */

const ruleVisible = ref(false);
const ruleSaving = ref(false);
const ruleForm = reactive({ enabled: true, rate: 1 });

async function openRule() {
  const rule = await getPointsRule();
  ruleForm.enabled = rule.enabled;
  ruleForm.rate = rule.rate ?? 1;
  ruleVisible.value = true;
}

async function saveRule() {
  ruleSaving.value = true;
  try {
    await updatePointsRule({
      enabled: ruleForm.enabled,
      rate: ruleForm.rate ?? 0,
    });
    ElMessage.success('积分规则已保存，新支付的订单按新规则结算');
    ruleVisible.value = false;
  } finally {
    ruleSaving.value = false;
  }
}
</script>

<template>
  <Page auto-content-height>
    <PointAdjustDrawer @success="gridApi.query()" />
    <Grid :table-title="$t('member.points.list')">
      <template #toolbar-tools>
        <ElButton @click="openRule">积分规则</ElButton>
        <ElButton type="primary" @click="onAdjust">
          <Plus class="size-5" />
          {{ $t('member.points.adjustAny') }}
        </ElButton>
      </template>
    </Grid>

    <ElDialog v-model="ruleVisible" title="积分规则" width="480px">
      <ElForm label-width="120px">
        <ElFormItem label="发放积分">
          <ElSwitch v-model="ruleForm.enabled" />
          <span class="rule-tip">关闭后订单支付成功不再发放积分</span>
        </ElFormItem>
        <ElFormItem label="每元积分">
          <ElInputNumber
            v-model="ruleForm.rate"
            :min="0"
            :max="100"
            :step="1"
            :disabled="!ruleForm.enabled"
            controls-position="right"
          />
          <span class="rule-tip">实付 100 元得 {{ 100 * (ruleForm.rate ?? 0) }} 分</span>
        </ElFormItem>
      </ElForm>
      <div class="rule-note">
        积分与累计消费在订单支付成功时结算，超时关单 / 取消不影响已发放部分；
        累计消费达到等级门槛会自动升级会员等级。<br />
        等级折扣开关在「会员管理 - 等级权益」页顶部维护，退款确认打款时按退款金额比例扣回积分与累计消费。
      </div>
      <template #footer>
        <ElButton @click="ruleVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="ruleSaving" @click="saveRule">
          保存
        </ElButton>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.rule-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.rule-note {
  padding: 12px;
  font-size: 12px;
  line-height: 20px;
  color: #909399;
  background-color: #fafafa;
  border-radius: 6px;
}
</style>
