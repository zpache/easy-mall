<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

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

import {
  getLogisticsConfig,
  updateLogisticsConfig,
} from '#/api/settings/logistics';

const form = reactive({
  kuaidi100Enabled: false,
  kuaidi100Customer: '',
  kuaidi100Key: '',
});

/** 敏感项是否已配置（决定输入框占位文案） */
const kuaidi100KeySet = ref(false);
const loading = ref(false);
const saving = ref(false);

async function loadConfig() {
  loading.value = true;
  try {
    const config = await getLogisticsConfig();
    form.kuaidi100Enabled = config.kuaidi100Enabled;
    form.kuaidi100Customer = config.kuaidi100Customer ?? '';
    kuaidi100KeySet.value = config.kuaidi100KeySet;
    // 敏感项不回显明文，留空即「不修改」
    form.kuaidi100Key = '';
  } finally {
    loading.value = false;
  }
}

onMounted(loadConfig);

function buildPayload() {
  return {
    kuaidi100Enabled: form.kuaidi100Enabled,
    kuaidi100Customer: form.kuaidi100Customer.trim(),
    kuaidi100Key: form.kuaidi100Key.trim() || undefined,
  };
}

function validate(): boolean {
  if (!form.kuaidi100Enabled) {
    return true;
  }
  const missing: string[] = [];
  if (!form.kuaidi100Customer.trim()) missing.push('快递100 客户编号');
  if (!kuaidi100KeySet.value && !form.kuaidi100Key.trim())
    missing.push('快递100 授权密钥');
  if (missing.length > 0) {
    ElMessage.warning(`启用物流查询前请先补全：${missing.join('、')}`);
    return false;
  }
  return true;
}

async function onSave() {
  if (!validate()) return;
  saving.value = true;
  try {
    await updateLogisticsConfig(buildPayload());
    ElMessage.success('物流配置已保存');
    await loadConfig();
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Page>
    <ElCard v-loading="loading" shadow="never" class="mb-4">
      <template #header>
        <div class="card-header">
          <span>快递100 物流查询</span>
          <span class="header-tip">
            开启后，管理后台与小程序订单详情可查看物流轨迹
          </span>
        </div>
      </template>
      <ElAlert type="info" :closable="false" show-icon class="mb-4">
        参数来自快递100 企业版「我的信息」页：客户编号（customer）与授权密钥（key）。
        每次查询消耗一条实时查询额度，订单详情每次打开都会实时查询。
      </ElAlert>
      <ElForm label-width="180px" class="max-w-[720px]">
        <ElFormItem label="启用物流查询">
          <ElSwitch v-model="form.kuaidi100Enabled" />
          <span class="form-tip">开启前需要先填完整客户编号与密钥</span>
        </ElFormItem>
        <ElFormItem label="客户编号 customer">
          <ElInput
            v-model="form.kuaidi100Customer"
            placeholder="快递100 授权客户编号"
            clearable
          />
        </ElFormItem>
        <ElFormItem label="授权密钥 key">
          <ElInput
            v-model="form.kuaidi100Key"
            type="password"
            show-password
            :placeholder="
              kuaidi100KeySet ? '已配置，留空表示不修改' : '快递100 授权密钥'
            "
          />
          <div class="form-tip">
            密钥仅用于服务端查询签名，接口返回时一律脱敏
          </div>
        </ElFormItem>
        <ElFormItem>
          <ElButton
            type="primary"
            :loading="saving"
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
