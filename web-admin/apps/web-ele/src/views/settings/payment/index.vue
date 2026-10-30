<script lang="ts" setup>
import type { SettingsPaymentApi } from '#/api/settings/payment';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  checkPaymentConfig,
  getPaymentConfig,
  updatePaymentConfig,
} from '#/api/settings/payment';

const form = reactive({
  wxpayEnabled: false,
  mockPayEnabled: true,
  appId: '',
  mchId: '',
  mchSerialNo: '',
  apiV3Key: '',
  privateKey: '',
  notifyUrl: '',
  wxpayPublicKey: '',
  wxpayPublicKeyId: '',
  refundNotifyUrl: '',
  orderPayTimeoutMinutes: 30,
  autoReceiveDays: 15,
});

/** 敏感项是否已配置（决定输入框占位文案） */
const apiV3KeySet = ref(false);
const privateKeySet = ref(false);
const wxpayPublicKeySet = ref(false);
const loading = ref(false);
const saving = ref(false);
const checking = ref(false);
const checkItems = ref<SettingsPaymentApi.ConfigCheckItem[]>([]);

const failCount = computed(
  () => checkItems.value.filter((item) => item.status === 'FAIL').length,
);
const warnCount = computed(
  () => checkItems.value.filter((item) => item.status === 'WARN').length,
);

async function loadConfig() {
  loading.value = true;
  try {
    const config = await getPaymentConfig();
    form.wxpayEnabled = config.wxpayEnabled;
    form.mockPayEnabled = config.mockPayEnabled;
    form.appId = config.appId ?? '';
    form.mchId = config.mchId ?? '';
    form.mchSerialNo = config.mchSerialNo ?? '';
    form.notifyUrl = config.notifyUrl ?? '';
    form.wxpayPublicKeyId = config.wxpayPublicKeyId ?? '';
    form.refundNotifyUrl = config.refundNotifyUrl ?? '';
    form.orderPayTimeoutMinutes = config.orderPayTimeoutMinutes ?? 30;
    form.autoReceiveDays = config.autoReceiveDays ?? 15;
    apiV3KeySet.value = config.apiV3KeySet;
    privateKeySet.value = config.privateKeySet;
    wxpayPublicKeySet.value = config.wxpayPublicKeySet;
    // 敏感项不回显明文，留空即「不修改」
    form.apiV3Key = '';
    form.privateKey = '';
    form.wxpayPublicKey = '';
  } finally {
    loading.value = false;
  }
}

onMounted(loadConfig);

function buildPayload(): SettingsPaymentApi.PaymentConfigPayload {
  return {
    wxpayEnabled: form.wxpayEnabled,
    mockPayEnabled: form.mockPayEnabled,
    appId: form.appId.trim(),
    mchId: form.mchId.trim(),
    mchSerialNo: form.mchSerialNo.trim(),
    notifyUrl: form.notifyUrl.trim(),
    wxpayPublicKeyId: form.wxpayPublicKeyId.trim(),
    refundNotifyUrl: form.refundNotifyUrl.trim(),
    orderPayTimeoutMinutes: form.orderPayTimeoutMinutes ?? 30,
    autoReceiveDays: form.autoReceiveDays ?? 15,
    apiV3Key: form.apiV3Key.trim() || undefined,
    privateKey: form.privateKey.trim() || undefined,
    wxpayPublicKey: form.wxpayPublicKey.trim() || undefined,
  };
}

function validate(): boolean {
  if (!form.wxpayEnabled && !form.mockPayEnabled) {
    ElMessage.warning('至少需要启用一个支付渠道，否则用户无法付款');
    return false;
  }
  if (form.wxpayEnabled) {
    const missing: string[] = [];
    if (!form.appId.trim()) missing.push('小程序 AppID');
    if (!form.mchId.trim()) missing.push('商户号');
    if (!form.mchSerialNo.trim()) missing.push('商户证书序列号');
    if (!apiV3KeySet.value && !form.apiV3Key.trim()) missing.push('APIv3 密钥');
    if (!privateKeySet.value && !form.privateKey.trim()) missing.push('商户 API 私钥');
    if (!form.notifyUrl.trim()) missing.push('支付回调地址');
    if (!wxpayPublicKeySet.value && !form.wxpayPublicKey.trim())
      missing.push('微信支付公钥');
    if (missing.length > 0) {
      ElMessage.warning(`启用微信支付前请先补全：${missing.join('、')}`);
      return false;
    }
  }
  return true;
}

async function onSave() {
  if (!validate()) return;
  saving.value = true;
  try {
    await updatePaymentConfig(buildPayload());
    ElMessage.success('支付配置已保存');
    await loadConfig();
    if (checkItems.value.length > 0) {
      checkItems.value = await checkPaymentConfig();
    }
  } finally {
    saving.value = false;
  }
}

async function onCheck() {
  if (!validate()) return;
  checking.value = true;
  try {
    // 自检读的是已落库的配置，先保存再校验，避免展示过期结果
    await updatePaymentConfig(buildPayload());
    await loadConfig();
    checkItems.value = await checkPaymentConfig();
    const failed = checkItems.value.filter((item) => item.status === 'FAIL');
    if (failed.length > 0) {
      ElMessage.warning(`自检未通过，共 ${failed.length} 项需要处理`);
    } else {
      ElMessage.success('参数自检通过');
    }
  } finally {
    checking.value = false;
  }
}

const statusMap: Record<
  string,
  { text: string; type: 'danger' | 'info' | 'success' | 'warning' }
> = {
  PASS: { text: '通过', type: 'success' },
  WARN: { text: '提醒', type: 'warning' },
  FAIL: { text: '未通过', type: 'danger' },
  SKIP: { text: '跳过', type: 'info' },
};
</script>

<template>
  <Page>
    <ElCard v-loading="loading" shadow="never" class="mb-4">
      <template #header>
        <div class="card-header">
          <span>支付渠道</span>
          <span class="header-tip">
            两个渠道至少启用一个；正式收款请只保留微信支付
          </span>
        </div>
      </template>
      <ElAlert type="info" :closable="false" show-icon class="mb-4">
        当前版本小程序端已支持「模拟支付」，微信支付正式流程（JSAPI 下单 / 回调幂等 /
        超时关单）将在下一期上线；先把商户参数配好，届时只需打开开关即可切换。
      </ElAlert>
      <ElForm label-width="150px" class="max-w-[720px]">
        <ElFormItem label="微信支付">
          <ElSwitch v-model="form.wxpayEnabled" />
          <span class="form-tip">
            开启后需要商户参数完整，否则保存会被拦截
          </span>
        </ElFormItem>
        <ElFormItem label="模拟支付">
          <ElSwitch v-model="form.mockPayEnabled" />
          <span class="form-tip">
            仅开发联调使用；关闭后小程序「去支付」将不可用
          </span>
        </ElFormItem>
      </ElForm>
    </ElCard>

    <ElCard shadow="never" class="mb-4">
      <template #header>
        <div class="card-header">
          <span>微信支付参数</span>
          <span class="header-tip">来自微信支付商户平台「账户中心 - API 安全」</span>
        </div>
      </template>
      <ElForm label-width="150px" class="max-w-[720px]">
        <ElFormItem label="小程序 AppID">
          <ElInput
            v-model="form.appId"
            placeholder="wx 开头，需与小程序主体一致"
            clearable
          />
        </ElFormItem>
        <ElFormItem label="商户号 mchId">
          <ElInput v-model="form.mchId" placeholder="8~10 位数字" clearable />
        </ElFormItem>
        <ElFormItem label="证书序列号">
          <ElInput
            v-model="form.mchSerialNo"
            placeholder="商户 API 证书序列号（十六进制）"
            clearable
          />
        </ElFormItem>
        <ElFormItem label="APIv3 密钥">
          <ElInput
            v-model="form.apiV3Key"
            type="password"
            show-password
            :placeholder="
              apiV3KeySet ? '已配置，留空表示不修改' : '32 位字符，用于回调解密'
            "
          />
        </ElFormItem>
        <ElFormItem label="商户 API 私钥">
          <ElInput
            v-model="form.privateKey"
            type="textarea"
            :rows="4"
            :placeholder="
              privateKeySet
                ? '已配置，留空表示不修改'
                : '粘贴 apiclient_key.pem 全部内容（含 BEGIN/END 行）'
            "
          />
          <div class="form-tip">
            私钥仅用于服务端下单签名，接口返回时一律脱敏
          </div>
        </ElFormItem>
        <ElFormItem label="微信支付公钥">
          <ElInput
            v-model="form.wxpayPublicKey"
            type="textarea"
            :rows="4"
            :placeholder="
              wxpayPublicKeySet
                ? '已配置，留空表示不修改'
                : '粘贴 pub_key.pem 全部内容（含 BEGIN/END 行）'
            "
          />
          <div class="form-tip">
            用于验证支付回调签名，缺失时回调会被拒绝、订单无法自动入账
          </div>
        </ElFormItem>
        <ElFormItem label="微信支付公钥 ID">
          <ElInput
            v-model="form.wxpayPublicKeyId"
            placeholder="PUB_KEY_ID_ 开头，可选"
            clearable
          />
          <div class="form-tip">与公钥配套，便于排查回调问题</div>
        </ElFormItem>
        <ElFormItem label="支付回调地址">
          <ElInput
            v-model="form.notifyUrl"
            placeholder="https://你的域名/api/pay/wxpay/notify"
            clearable
          />
          <div class="form-tip">微信支付只回调 https 地址，且必须公网可访问</div>
        </ElFormItem>
        <ElFormItem label="退款回调地址">
          <ElInput
            v-model="form.refundNotifyUrl"
            placeholder="https://你的域名/api/pay/wxpay/refund-notify（可留空）"
            clearable
          />
          <div class="form-tip">
            留空则退款结果只依赖主动查询，配置后微信会推送退款状态
          </div>
        </ElFormItem>
        <ElFormItem label="支付超时（分钟）">
          <ElInputNumber
            v-model="form.orderPayTimeoutMinutes"
            :min="1"
            :max="1440"
            :step="5"
            controls-position="right"
            class="!w-40"
          />
          <span class="form-tip">超时未支付自动关单，默认 30 分钟</span>
        </ElFormItem>
        <ElFormItem label="自动确认收货">
          <ElInputNumber
            v-model="form.autoReceiveDays"
            :min="0"
            :max="90"
            :step="1"
            controls-position="right"
            class="!w-40"
          />
          <span class="form-tip">
            发货后 N 天未手动确认则自动完成，0 表示关闭（需用户自己点确认收货）
          </span>
        </ElFormItem>
        <ElFormItem>
          <ElButton type="primary" :loading="saving" @click="onSave">
            保存配置
          </ElButton>
          <ElButton :loading="checking" @click="onCheck">
            参数自检
          </ElButton>
        </ElFormItem>
      </ElForm>
    </ElCard>

    <ElCard shadow="never">
      <template #header>
        <div class="card-header">
          <span>参数自检结果</span>
          <span class="header-tip">
            点击「参数自检」后展示；未通过项需修正后才能启用微信支付
          </span>
        </div>
      </template>
      <div v-if="checkItems.length === 0" class="empty-tip">
        还没有做过自检，点击上方「参数自检」按当前已保存的配置逐项检查
      </div>
      <template v-else>
        <ElAlert
          :type="failCount > 0 ? 'error' : warnCount > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
          class="mb-4"
        >
          {{
            failCount > 0
              ? `有 ${failCount} 项未通过，支付参数尚不可用`
              : warnCount > 0
                ? `全部必填项已通过，另有 ${warnCount} 项提醒`
                : '全部检查项通过，支付参数可用于正式收款'
          }}
        </ElAlert>
        <div
          v-for="item in checkItems"
          :key="item.key"
          class="check-row"
        >
          <ElTag :type="statusMap[item.status]?.type ?? 'info'" size="small">
            {{ statusMap[item.status]?.text ?? item.status }}
          </ElTag>
          <span class="check-label">{{ item.label }}</span>
          <span class="check-message">{{ item.message }}</span>
        </div>
      </template>
    </ElCard>
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
  font-weight: 400;
  color: #909399;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.empty-tip {
  padding: 12px 0;
  font-size: 13px;
  color: #909399;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px dashed #ebeef5;
}

.check-row:last-child {
  border-bottom: none;
}

.check-label {
  min-width: 130px;
  font-weight: 500;
}

.check-message {
  color: #909399;
}
</style>
