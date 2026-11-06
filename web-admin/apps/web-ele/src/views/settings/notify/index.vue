<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSwitch,
} from 'element-plus';

import { getNotifyConfig, updateNotifyConfig } from '#/api/settings/notify';
import { SETTING_PERMISSIONS } from '#/constants/permissions';
import { usePermission } from '#/composables/use-permission';

const { can } = usePermission();
const canEdit = computed(() => can(SETTING_PERMISSIONS.notifyUpdate));

const form = reactive({
  subscribeEnabled: false,
  wxAppId: '',
  wxAppSecret: '',
  tmplDeliver: '',
  tmplRefund: '',
});

/** 敏感项是否已配置（决定输入框占位文案） */
const wxAppSecretSet = ref(false);
const loading = ref(false);
const saving = ref(false);

async function loadConfig() {
  loading.value = true;
  try {
    const config = await getNotifyConfig();
    form.subscribeEnabled = config.subscribeEnabled;
    form.wxAppId = config.wxAppId ?? '';
    form.tmplDeliver = config.tmplDeliver ?? '';
    form.tmplRefund = config.tmplRefund ?? '';
    wxAppSecretSet.value = config.wxAppSecretSet;
    // 敏感项回显脱敏值，留空未配置
    form.wxAppSecret = config.wxAppSecretSet ? (config.wxAppSecret ?? '') : '';
  } finally {
    loading.value = false;
  }
}

onMounted(loadConfig);

/** 脱敏占位值：保持原样提交表示「不修改」 */
const SECRET_MASK = '********';

function buildPayload() {
  const secret = form.wxAppSecret.trim();
  return {
    subscribeEnabled: form.subscribeEnabled,
    wxAppId: form.wxAppId.trim(),
    // 未改动的脱敏值不回传，避免把掩码当成新密钥落库
    wxAppSecret: secret && secret !== SECRET_MASK ? secret : undefined,
    tmplDeliver: form.tmplDeliver.trim(),
    tmplRefund: form.tmplRefund.trim(),
  };
}

function validate(): boolean {
  if (!form.subscribeEnabled) {
    return true;
  }
  const missing: string[] = [];
  if (!form.wxAppId.trim()) missing.push('小程序 AppID');
  if (!wxAppSecretSet.value && !form.wxAppSecret.trim()) missing.push('AppSecret');
  if (missing.length > 0) {
    ElMessage.warning(`启用消息推送前请先补全：${missing.join('、')}`);
    return false;
  }
  return true;
}

async function onSave() {
  if (!canEdit.value || !validate()) return;
  saving.value = true;
  try {
    await updateNotifyConfig(buildPayload());
    ElMessage.success('通知配置已保存');
    await loadConfig();
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Page>
    <ElAlert
      v-if="!canEdit"
      class="mb-4"
      :closable="false"
      show-icon
      title="当前账号仅可查看通知配置，无法保存修改"
      type="info"
    />
    <ElCard v-loading="loading" shadow="never" class="mb-4">
      <template #header>
        <div class="card-header">
          <span>微信订阅消息</span>
          <span class="header-tip">发货、退款结果自动推送到用户微信</span>
        </div>
      </template>
      <ElAlert type="info" :closable="false" show-icon class="mb-4">
        使用前请先在小程序后台「功能 → 订阅消息」选用模板，并把模板 ID 填到下方。
        服务端按模板字段类型（订单号 / 商品 / 时间 / 状态 / 金额 / 快递）自动填充，
        模板字段顺序不限。小程序端在用户支付成功时请求订阅授权。
      </ElAlert>
      <ElForm label-width="180px" class="max-w-[720px]">
        <ElFormItem label="启用消息推送">
          <ElSwitch v-model="form.subscribeEnabled" :disabled="!canEdit" />
          <span class="form-tip">开启前需要先填完整 AppID 与 AppSecret</span>
        </ElFormItem>
        <ElFormItem label="小程序 AppID">
          <ElInput
            v-model="form.wxAppId"
            :disabled="!canEdit"
            placeholder="wx 开头，须与登录/支付所用主体一致"
            clearable
          />
        </ElFormItem>
        <ElFormItem label="AppSecret">
          <ElInput
            v-model="form.wxAppSecret"
            :disabled="!canEdit"
            type="password"
            show-password
            :placeholder="
              wxAppSecretSet ? '已配置，留空表示不修改' : '小程序后台「开发管理」中获取'
            "
          />
          <div class="form-tip">
            仅用于服务端获取接口调用凭据，接口返回时一律脱敏
          </div>
        </ElFormItem>
        <ElFormItem label="发货提醒模板 ID">
          <ElInput
            v-model="form.tmplDeliver"
            :disabled="!canEdit"
            placeholder="小程序后台申请的发货提醒模板，留空则不推送"
            clearable
          />
        </ElFormItem>
        <ElFormItem label="退款结果模板 ID">
          <ElInput
            v-model="form.tmplRefund"
            :disabled="!canEdit"
            placeholder="小程序后台申请的退款结果模板，留空则不推送"
            clearable
          />
        </ElFormItem>
        <ElFormItem>
          <ElButton
            v-access:code="SETTING_PERMISSIONS.notifyUpdate"
            :loading="saving"
            type="primary"
            @click="onSave"
          >
            保存配置
          </ElButton>
        </ElFormItem>
      </ElForm>
    </ElCard>
  </Page>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-tip {
  font-size: 12px;
  color: hsl(var(--foreground) / 50%);
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: hsl(var(--foreground) / 50%);
}
</style>
