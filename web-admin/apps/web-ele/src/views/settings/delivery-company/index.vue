<script lang="ts" setup>
import type { SettingsDeliveryApi } from '#/api/settings/delivery';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElLink,
  ElMessage,
  ElMessageBox,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  createDeliveryCompany,
  deleteDeliveryCompany,
  getDeliveryCompanies,
  updateDeliveryCompany,
} from '#/api/settings/delivery';

/** 快递100 官方「常用快递公司编码」表 */
const CODE_DOC_URL =
  'https://www.kuaidi100.com/ecommerce/api/index.html?name=42';

const list = ref<SettingsDeliveryApi.DeliveryCompany[]>([]);
const loading = ref(false);

const dialogVisible = ref(false);
const saving = ref(false);
/** 编辑中的 id，null 表示新增 */
const editingId = ref<null | number>(null);

const form = reactive({
  name: '',
  code: '',
  phoneRequired: false,
  sort: 0,
  status: 1,
  remark: '',
});

async function loadList() {
  loading.value = true;
  try {
    list.value = await getDeliveryCompanies();
  } finally {
    loading.value = false;
  }
}

onMounted(loadList);

function resetForm() {
  editingId.value = null;
  form.name = '';
  form.code = '';
  form.phoneRequired = false;
  form.sort = (list.value.length + 1) * 10;
  form.status = 1;
  form.remark = '';
}

function onCreate() {
  resetForm();
  dialogVisible.value = true;
}

function onEdit(row: SettingsDeliveryApi.DeliveryCompany) {
  editingId.value = row.id;
  form.name = row.name;
  form.code = row.code;
  form.phoneRequired = row.phoneRequired;
  form.sort = row.sort;
  form.status = row.status;
  form.remark = row.remark ?? '';
  dialogVisible.value = true;
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写快递公司名称');
    return;
  }
  if (!form.code.trim()) {
    ElMessage.warning('请填写快递100 公司编码');
    return;
  }
  saving.value = true;
  try {
    const payload: SettingsDeliveryApi.DeliveryCompanyPayload = {
      name: form.name.trim(),
      code: form.code.trim(),
      phoneRequired: form.phoneRequired,
      sort: form.sort ?? 0,
      status: form.status,
      remark: form.remark.trim() || null,
    };
    if (editingId.value === null) {
      await createDeliveryCompany(payload);
      ElMessage.success('快递公司已添加');
    } else {
      await updateDeliveryCompany(editingId.value, payload);
      ElMessage.success('快递公司已保存');
    }
    dialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: SettingsDeliveryApi.DeliveryCompany) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」吗？删除后发货下拉不再出现，历史订单的物流轨迹仍按已存的编码查询。`,
      '删除确认',
      { type: 'warning' },
    );
  } catch {
    return;
  }
  await deleteDeliveryCompany(row.id);
  ElMessage.success('已删除');
  await loadList();
}

async function onToggleStatus(
  row: SettingsDeliveryApi.DeliveryCompany,
  value: boolean,
) {
  await updateDeliveryCompany(row.id, {
    name: row.name,
    code: row.code,
    phoneRequired: row.phoneRequired,
    sort: row.sort,
    status: value ? 1 : 0,
    remark: row.remark ?? null,
  });
  ElMessage.success(value ? '已启用' : '已停用，发货下拉不再出现');
  await loadList();
}
</script>

<template>
  <Page>
    <ElCard shadow="never">
      <template #header>
        <div class="card-header">
          <span>快递公司</span>
          <span class="header-tip">
            发货弹窗的下拉与物流轨迹查询的编码解析都读这份名单；新增快递无需改代码发版
          </span>
          <ElLink
            type="primary"
            :href="CODE_DOC_URL"
            target="_blank"
            class="header-link"
          >
            快递100 官方编码表 ↗
          </ElLink>
          <ElButton type="primary" size="small" @click="onCreate">
            新增快递公司
          </ElButton>
        </div>
      </template>
      <ElTable
        v-loading="loading"
        :data="list"
        row-key="id"
        empty-text="暂无快递公司，请先新增"
      >
        <ElTableColumn prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <span :class="{ 'text-gray-400': row.status === 0 }">
              {{ row.name }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="code" label="快递100 编码" width="160">
          <template #default="{ row }">
            <code class="code-text">{{ row.code }}</code>
          </template>
        </ElTableColumn>
        <ElTableColumn label="手机号后四位" width="140">
          <template #default="{ row }">
            <ElTag
              :type="row.phoneRequired ? 'warning' : 'info'"
              size="small"
            >
              {{ row.phoneRequired ? '必须传' : '不需要' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="sort" label="排序" width="90" />
        <ElTableColumn label="状态" width="110">
          <template #default="{ row }">
            <ElSwitch
              :model-value="row.status === 1"
              @change="(v: boolean | number | string) => onToggleStatus(row, Boolean(v))"
            />
          </template>
        </ElTableColumn>
        <ElTableColumn prop="remark" label="备注" min-width="180">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="updatedAt" label="更新时间" width="170" />
        <ElTableColumn label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <ElButton link type="primary" size="small" @click="onEdit(row)">
              编辑
            </ElButton>
            <ElButton link type="danger" size="small" @click="onDelete(row)">
              删除
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog
      v-model="dialogVisible"
      :title="editingId === null ? '新增快递公司' : '编辑快递公司'"
      width="480px"
      :close-on-click-modal="false"
    >
      <ElForm label-width="120px">
        <ElFormItem label="公司名称" required>
          <ElInput
            v-model="form.name"
            placeholder="如 顺丰速运，发货单和订单详情显示的就是它"
            maxlength="50"
          />
        </ElFormItem>
        <ElFormItem label="快递100 编码" required>
          <ElInput
            v-model="form.code"
            placeholder="如 shunfeng"
            maxlength="32"
          />
          <div class="form-tip">
            快递100 的公司编码，查询轨迹时使用；可对照快递100 官方编码表填写
          </div>
        </ElFormItem>
        <ElFormItem label="手机号后四位">
          <ElSwitch v-model="form.phoneRequired" />
          <span class="form-tip">
            顺丰、EMS、京东等部分快递查询时必须校验收件人手机号后四位
          </span>
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber
            v-model="form.sort"
            :min="0"
            :step="10"
            controls-position="right"
            class="!w-40"
          />
          <span class="form-tip">越小越靠前</span>
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSwitch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
          />
          <span class="form-tip">停用后不出现在发货下拉，历史订单不受影响</span>
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            placeholder="选填，如合作的网点/月结信息"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="onSave">
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

.header-link {
  font-size: 12px;
}

.code-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgb(0 0 0 / 5%);
}
</style>
