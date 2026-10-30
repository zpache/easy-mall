<script lang="ts" setup>
import type { SettingsFreightApi } from '#/api/settings/freight';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElCascader,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElRadioButton,
  ElRadioGroup,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  createFreightRule,
  deleteFreightRule,
  getFreightDetail,
  getRegionTree,
  updateFreightConfig,
  updateFreightRule,
} from '#/api/settings/freight';

/* ---------------- 全局运费配置 ---------------- */

const configForm = reactive({
  defaultFee: 0,
  /** 空值表示不设包邮门槛；提交时统一转成 null 给后端 */
  freeAmount: undefined as number | undefined,
});
const configVersion = ref<number | undefined>();
const configSaving = ref(false);

/* ---------------- 区域规则 ---------------- */

const rules = ref<SettingsFreightApi.FreightRule[]>([]);
const rulesLoading = ref(false);

/* ---------------- 规则新增/编辑弹窗 ---------------- */

const ruleDialogVisible = ref(false);
const ruleSaving = ref(false);
/** 编辑中的规则 id，null 表示新增 */
const editingRuleId = ref<null | number>(null);
const ruleForm = reactive({
  regionCode: '',
  deliverable: 1 as 0 | 1,
  fee: 0,
  /** 空值表示不设包邮门槛；提交时统一转成 null 给后端 */
  freeAmount: undefined as number | undefined,
});
const ruleVersion = ref<number | undefined>();

/** 省市两级区域树（级联选择器：任意层级可选） */
const regionOptions = ref<SettingsFreightApi.RegionNode[]>([]);

async function loadDetail() {
  rulesLoading.value = true;
  try {
    const detail = await getFreightDetail();
    configForm.defaultFee = Number(detail.defaultFee ?? 0);
    configForm.freeAmount =
      detail.freeAmount === null || detail.freeAmount === undefined
        ? undefined
        : Number(detail.freeAmount);
    configVersion.value = detail.version;
    rules.value = detail.rules ?? [];
  } finally {
    rulesLoading.value = false;
  }
}

async function loadRegions() {
  regionOptions.value = await getRegionTree();
}

onMounted(() => {
  loadDetail();
  loadRegions();
});

async function onSaveConfig() {
  configSaving.value = true;
  try {
    await updateFreightConfig({
      defaultFee: configForm.defaultFee ?? 0,
      freeAmount: configForm.freeAmount ?? null,
      version: configVersion.value,
    });
    ElMessage.success('运费配置已保存');
    await loadDetail();
  } finally {
    configSaving.value = false;
  }
}

function feeText(row: SettingsFreightApi.FreightRule) {
  if (!row.deliverable) return '-';
  return Number(row.fee) === 0 ? '包邮' : `¥${Number(row.fee).toFixed(2)}`;
}

function freeAmountText(row: SettingsFreightApi.FreightRule) {
  if (!row.deliverable) return '-';
  if (Number(row.fee) === 0) return '-';
  return row.freeAmount === null || row.freeAmount === undefined
    ? '-'
    : `满${Number(row.freeAmount)}元包邮`;
}

function onCreateRule() {
  editingRuleId.value = null;
  ruleForm.regionCode = '';
  ruleForm.deliverable = 1;
  ruleForm.fee = 0;
  ruleForm.freeAmount = undefined;
  ruleVersion.value = undefined;
  ruleDialogVisible.value = true;
}

function onEditRule(row: SettingsFreightApi.FreightRule) {
  editingRuleId.value = row.id;
  ruleForm.regionCode = row.regionCode;
  ruleForm.deliverable = row.deliverable;
  ruleForm.fee = Number(row.fee);
  ruleForm.freeAmount =
    row.freeAmount === null || row.freeAmount === undefined
      ? undefined
      : Number(row.freeAmount);
  ruleVersion.value = row.version;
  ruleDialogVisible.value = true;
}

async function onDeleteRule(row: SettingsFreightApi.FreightRule) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.regionPath}」的运费规则吗？删除后该地区将按统一运费计费。`,
      '删除确认',
      { type: 'warning' },
    );
  } catch {
    return;
  }
  await deleteFreightRule(row.id);
  ElMessage.success('规则已删除');
  await loadDetail();
}

async function onSaveRule() {
  if (!ruleForm.regionCode) {
    ElMessage.warning('请选择地区');
    return;
  }
  if (ruleForm.deliverable === 1 && (ruleForm.fee ?? 0) < 0) {
    ElMessage.warning('运费不能小于 0');
    return;
  }
  ruleSaving.value = true;
  try {
    const payload = {
      deliverable: ruleForm.deliverable,
      fee: ruleForm.deliverable === 1 ? (ruleForm.fee ?? 0) : 0,
      freeAmount:
        ruleForm.deliverable === 1 ? (ruleForm.freeAmount ?? null) : null,
      regionCode: ruleForm.regionCode,
      version: ruleVersion.value,
    };
    if (editingRuleId.value === null) {
      await createFreightRule(payload);
    } else {
      await updateFreightRule(editingRuleId.value, payload);
    }
    ElMessage.success(editingRuleId.value === null ? '规则已添加' : '规则已保存');
    ruleDialogVisible.value = false;
    await loadDetail();
  } finally {
    ruleSaving.value = false;
  }
}
</script>

<template>
  <Page>
    <!-- 全局运费配置 -->
    <ElCard shadow="never" class="mb-4">
      <template #header>
        <div class="card-header">
          <span>基础运费设置</span>
          <span class="header-tip">未命中区域规则的地区按统一运费计费</span>
        </div>
      </template>
      <ElForm label-width="140px" class="max-w-[560px]">
        <ElFormItem label="统一运费（元）">
          <ElInputNumber
            v-model="configForm.defaultFee"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            class="!w-48"
          />
          <span class="form-tip">填 0 表示未设置地区默认包邮</span>
        </ElFormItem>
        <ElFormItem label="全国满额包邮（元）">
          <ElInputNumber
            v-model="configForm.freeAmount"
            :min="0"
            :precision="2"
            :step="10"
            controls-position="right"
            placeholder="不启用"
            class="!w-48"
          />
          <span class="form-tip">留空或不填表示不启用；仅对按统一运费计费的地区生效</span>
        </ElFormItem>
        <ElFormItem>
          <ElButton
            type="primary"
            :loading="configSaving"
            @click="onSaveConfig"
          >
            保存配置
          </ElButton>
        </ElFormItem>
      </ElForm>
    </ElCard>

    <!-- 区域运费规则 -->
    <ElCard shadow="never">
      <template #header>
        <div class="card-header">
          <span>区域运费规则</span>
          <span class="header-tip">
            命中优先级：市规则 &gt; 省规则 &gt; 统一运费；运费填 0 即该地区无条件包邮
          </span>
          <ElButton type="primary" size="small" @click="onCreateRule">
            新增规则
          </ElButton>
        </div>
      </template>
      <ElTable
        v-loading="rulesLoading"
        :data="rules"
        row-key="id"
        empty-text="暂无区域规则，所有地区按统一运费计费"
      >
        <ElTableColumn prop="regionPath" label="地区" min-width="220" />
        <ElTableColumn prop="regionLevel" label="级别" width="90">
          <template #default="{ row }">
            <ElTag :type="row.regionLevel === 1 ? 'primary' : 'success'" size="small">
              {{ row.regionLevel === 1 ? '省' : '市' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="是否配送" width="110">
          <template #default="{ row }">
            <ElTag :type="row.deliverable === 1 ? 'info' : 'danger'" size="small">
              {{ row.deliverable === 1 ? '配送' : '不配送' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="运费" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-red-500': row.deliverable === 1 && Number(row.fee) === 0 }">
              {{ feeText(row) }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="满额包邮" width="140">
          <template #default="{ row }">{{ freeAmountText(row) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="updatedAt" label="更新时间" width="170" />
        <ElTableColumn label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <ElButton link type="primary" size="small" @click="onEditRule(row)">
              编辑
            </ElButton>
            <ElButton link type="danger" size="small" @click="onDeleteRule(row)">
              删除
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <!-- 规则新增/编辑弹窗 -->
    <ElDialog
      v-model="ruleDialogVisible"
      :title="editingRuleId === null ? '新增区域运费规则' : '编辑区域运费规则'"
      width="480px"
      :close-on-click-modal="false"
    >
      <ElForm label-width="110px">
        <ElFormItem label="地区" required>
          <ElCascader
            v-model="ruleForm.regionCode"
            :options="regionOptions"
            :props="{
              value: 'code',
              label: 'name',
              children: 'children',
              checkStrictly: true,
              emitPath: false,
            }"
            placeholder="选择省或市（市规则优先于省规则）"
            clearable
            filterable
            class="!w-full"
            :disabled="editingRuleId !== null"
          />
          <div v-if="editingRuleId !== null" class="form-tip">
            规则地区不可修改，如需更换请删除后重新新增
          </div>
        </ElFormItem>
        <ElFormItem label="是否配送">
          <ElRadioGroup v-model="ruleForm.deliverable">
            <ElRadioButton :value="1">配送</ElRadioButton>
            <ElRadioButton :value="0">不配送</ElRadioButton>
          </ElRadioGroup>
        </ElFormItem>
        <template v-if="ruleForm.deliverable === 1">
          <ElFormItem label="运费（元）">
            <ElInputNumber
              v-model="ruleForm.fee"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              class="!w-48"
            />
            <span class="form-tip">填 0 表示该地区无条件包邮</span>
          </ElFormItem>
          <ElFormItem v-if="Number(ruleForm.fee) > 0" label="满额包邮（元）">
            <ElInputNumber
              v-model="ruleForm.freeAmount"
              :min="0"
              :precision="2"
              :step="10"
              controls-position="right"
              placeholder="不启用"
              class="!w-48"
            />
            <span class="form-tip">达到该金额时此地区包邮</span>
          </ElFormItem>
        </template>
      </ElForm>
      <template #footer>
        <ElButton @click="ruleDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="ruleSaving" @click="onSaveRule">
          保存
        </ElButton>
      </template>
    </ElDialog>
  </Page>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-tip {
  font-size: 12px;
  color: #909399;
  flex: 1;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
