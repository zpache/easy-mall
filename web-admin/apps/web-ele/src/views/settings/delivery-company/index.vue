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
  ElOption,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTooltip,
} from 'element-plus';

import {
  createDeliveryCompany,
  deleteDeliveryCompany,
  getDeliveryCompanies,
  updateDeliveryCompany,
} from '#/api/settings/delivery';

/** 快递100 官方编码表（编码一律小写，如 shunfeng） */
const CODE_DOC_URL =
  'https://www.kuaidi100.com/ecommerce/api/index.html?name=42';
/** 接口文档里的编码表，做备用入口 */
const CODE_DOC_API_URL =
  'https://api.kuaidi100.com/document/603cb649a62a19500e19866b';

/**
 * 常用快递编码速查：取自快递100 官方编码表。
 * 选中后名称与编码一起填入，避免手打把 shunfeng 写成 sf 之类查不到轨迹。
 */
const COMMON_CODES: { code: string; name: string }[] = [
  { name: '顺丰速运', code: 'shunfeng' },
  { name: '京东物流', code: 'jd' },
  { name: '中通快递', code: 'zhongtong' },
  { name: '圆通速递', code: 'yuantong' },
  { name: '申通快递', code: 'shentong' },
  { name: '韵达快递', code: 'yunda' },
  { name: '邮政 EMS', code: 'ems' },
  { name: '极兔速递', code: 'jtexpress' },
  { name: '德邦快递', code: 'debangkuaidi' },
  { name: '德邦物流', code: 'debangwuliu' },
  { name: '百世快递', code: 'huitongkuaidi' },
  { name: '邮政快递包裹', code: 'youzhengguonei' },
  { name: '邮政标准快递', code: 'youzhengbk' },
  { name: '天天快递', code: 'tiantian' },
  { name: '宅急送', code: 'zhaijisong' },
  { name: '优速快递', code: 'youshuwuliu' },
  { name: '跨越速运', code: 'kuayue' },
  { name: '安能快运', code: 'annengwuliu' },
  { name: '顺心捷达', code: 'sxjdfreight' },
  { name: '中通快运', code: 'zhongtongkuaiyun' },
  { name: '圆通快运', code: 'yuantongkuaiyun' },
  { name: '韵达快运', code: 'yundakuaiyun' },
  { name: '顺丰快运', code: 'shunfengkuaiyun' },
  { name: '百世快运', code: 'baishiwuliu' },
  { name: '联邦快递', code: 'lianbangkuaidi' },
  { name: 'DHL', code: 'dhl' },
  { name: 'FedEx', code: 'fedex' },
  { name: 'UPS', code: 'ups' },
  { name: 'TNT', code: 'tnt' },
];

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

/** 速查下拉当前选中的编码，只用于一键填充，不提交 */
const quickCode = ref('');

function onQuickPick(code: string) {
  const hit = COMMON_CODES.find((item) => item.code === code);
  if (!hit) {
    return;
  }
  form.name = hit.name;
  form.code = hit.code;
}

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
  quickCode.value = '';
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
  quickCode.value = row.code;
  dialogVisible.value = true;
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写快递公司名称');
    return;
  }
  const code = form.code.trim();
  if (!code) {
    ElMessage.warning('请填写快递100 公司编码');
    return;
  }
  // 官方编码一律小写字母/数字，写成 SF、顺丰 之类查轨迹会直接失败
  if (!/^[a-z0-9]+$/.test(code)) {
    ElMessage.warning('编码只能是小写字母或数字，请对照快递100 官方编码表填写');
    return;
  }
  saving.value = true;
  try {
    const payload: SettingsDeliveryApi.DeliveryCompanyPayload = {
      name: form.name.trim(),
      code,
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
        <ElFormItem label="常用快递">
          <ElSelect
            v-model="quickCode"
            filterable
            clearable
            placeholder="搜索公司名或编码，选中后自动填入下面两项"
            class="!w-full"
            @change="onQuickPick"
          >
            <ElOption
              v-for="item in COMMON_CODES"
              :key="item.code"
              :label="`${item.name}（${item.code}）`"
              :value="item.code"
            />
          </ElSelect>
          <div class="form-tip">
            常见快递的官方编码已内置，选一下即可；表里没有的再手工填下面两项
          </div>
        </ElFormItem>
        <ElFormItem label="公司名称" required>
          <ElInput
            v-model="form.name"
            placeholder="如 顺丰速运，发货单和订单详情显示的就是它"
            maxlength="50"
          />
        </ElFormItem>
        <ElFormItem required>
          <template #label>
            <span class="label-with-help">
              快递100 编码
              <ElTooltip
                placement="top"
                content="编码是快递100 侧的公司标识，不是快递单号。一律小写字母或数字，如 shunfeng、zhongtong。填错会导致查不到物流轨迹。"
              >
                <span class="help-icon">?</span>
              </ElTooltip>
            </span>
          </template>
          <ElInput
            v-model="form.code"
            placeholder="如 shunfeng（小写，无空格）"
            maxlength="32"
          >
            <template #append>
              <ElLink
                type="primary"
                :href="CODE_DOC_URL"
                target="_blank"
                :underline="false"
              >
                查编码
              </ElLink>
            </template>
          </ElInput>
          <div class="form-tip form-tip-block">
            在哪查：快递100 官方「常用快递公司编码」表（点右侧「查编码」或上方链接），
            搜公司名就能看到对应编码；也可在快递100 接口文档「快递公司编码」章节查全量。
            <ElLink
              type="primary"
              :href="CODE_DOC_API_URL"
              target="_blank"
              :underline="false"
            >
              接口文档
            </ElLink>
          </div>
        </ElFormItem>
        <ElFormItem label="手机号后四位">
          <ElSwitch v-model="form.phoneRequired" />
          <span class="form-tip">
            顺丰、EMS 查询时必须校验收件人手机号后四位；其余快递不传，减少信息外传
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

/* 整行提示（较长的说明文字），不跟输入框挤在一行 */
.form-tip-block {
  display: block;
  width: 100%;
  margin: 4px 0 0;
  line-height: 1.6;
}

.header-link {
  font-size: 12px;
  margin-right: 4px;
}

.label-with-help {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.help-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  font-size: 11px;
  line-height: 1;
  color: #909399;
  cursor: help;
  border: 1px solid currentcolor;
  border-radius: 50%;
}

.code-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgb(0 0 0 / 5%);
}
</style>
