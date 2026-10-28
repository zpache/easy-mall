<script lang="ts" setup>
import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElButton, ElDialog, ElMessage } from 'element-plus';
import QRCode from 'qrcode';

const props = defineProps<{
  pageId?: number;
  pageName?: string;
  previewCode?: string;
  visible: boolean;
}>();

const emit = defineEmits<{ 'update:visible': [value: boolean] }>();

const qrDataUrl = ref('');
const previewUrl = computed(() =>
  props.previewCode
    ? `${window.location.origin}/decorate/preview/${props.previewCode}`
    : '',
);

async function renderQr() {
  if (!previewUrl.value) {
    qrDataUrl.value = '';
    return;
  }
  qrDataUrl.value = await QRCode.toDataURL(previewUrl.value, {
    color: { dark: '#1f2329', light: '#ffffff' },
    margin: 1,
    width: 240,
  });
}

watch(
  () => [props.visible, props.previewCode],
  ([visible]) => {
    if (visible) {
      void renderQr();
    }
  },
  { immediate: true },
);

async function copyLink() {
  if (!previewUrl.value) {
    return;
  }
  await navigator.clipboard.writeText(previewUrl.value);
  ElMessage.success('预览链接已复制');
}

function openPreview() {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank');
  }
}
</script>

<template>
  <ElDialog
    :model-value="visible"
    append-to-body
    title="真机扫码预览"
    width="420px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="preview-body">
      <div class="qr-wrap">
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="预览二维码" class="qr" />
        <div v-else class="qr placeholder">
          <IconifyIcon icon="lucide:qr-code" />
        </div>
      </div>
      <p class="title">{{ pageName || '未命名页面' }}</p>
      <p class="tip">用手机扫码即可在真机上查看当前装修效果（实时读取草稿）</p>
      <div class="url">
        <span class="url-text">{{ previewUrl || '保存后生成预览码' }}</span>
      </div>
      <div class="actions">
        <ElButton size="small" @click="copyLink">复制链接</ElButton>
        <ElButton size="small" type="primary" @click="openPreview">
          在浏览器预览
        </ElButton>
      </div>
    </div>
  </ElDialog>
</template>

<style scoped>
.preview-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0 8px;
}
.qr-wrap {
  padding: 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.qr {
  display: block;
  width: 220px;
  height: 220px;
}
.qr.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #dcdfe6;
  background: #f7f8fa;
}
.title {
  margin: 12px 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.tip {
  margin: 0 0 10px;
  font-size: 11px;
  color: #909399;
  text-align: center;
}
.url {
  width: 100%;
  padding: 6px 10px;
  overflow: hidden;
  text-align: center;
  background: #f7f8fa;
  border-radius: 4px;
}
.url-text {
  font-size: 11px;
  color: #606266;
  word-break: break-all;
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
</style>
