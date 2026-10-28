<script lang="ts" setup>
import { ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElButton, ElInput, ElMessage } from 'element-plus';

import { uploadImageApi } from '#/api/upload';

defineOptions({ name: 'NavIconField' });

const props = defineProps<{
  label?: string;
  modelValue?: string;
  /** 未选图时的占位图标 */
  placeholderIcon?: string;
}>();

const emit = defineEmits<{ 'update:modelValue': [value: string] }>();

const MAX_SIZE = 5 * 1024 * 1024;
const inputRef = ref<HTMLInputElement>();
const uploading = ref(false);
const progress = ref(0);

function pick() {
  if (!uploading.value) {
    inputRef.value?.click();
  }
}

async function onChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }
  if (file.size > MAX_SIZE) {
    ElMessage.warning('图标不能超过 5MB');
    return;
  }
  uploading.value = true;
  progress.value = 0;
  try {
    const result = await uploadImageApi(file, {
      onProgress: (percent) => {
        progress.value = percent;
      },
      scene: 'decorate-nav',
    });
    if (result?.url) {
      emit('update:modelValue', result.url);
    }
  } catch (error: any) {
    if (!error?.response && error?.message) {
      ElMessage.error(error.message);
    }
  } finally {
    uploading.value = false;
    progress.value = 0;
  }
}
</script>

<template>
  <div class="nav-icon-field">
    <div class="preview" :class="{ clickable: !uploading }" title="点击上传图标" @click="pick">
      <img v-if="props.modelValue" :src="props.modelValue" alt="icon" />
      <IconifyIcon v-else :icon="props.placeholderIcon || 'lucide:image'" class="ph" />
      <div v-if="uploading" class="mask">
        <IconifyIcon v-if="progress === 0" class="spin" icon="lucide:loader-2" />
        <span v-else>{{ progress }}%</span>
      </div>
    </div>
    <div class="side">
      <ElInput
        :model-value="props.modelValue"
        placeholder="图标地址，或点击左侧上传"
        size="small"
        @update:model-value="(val: string) => emit('update:modelValue', val)"
      />
      <div class="ops">
        <ElButton :loading="uploading" size="small" text type="primary" @click="pick">
          上传
        </ElButton>
        <ElButton v-if="props.modelValue" size="small" text @click="emit('update:modelValue', '')">
          清除
        </ElButton>
      </div>
    </div>
    <input
      ref="inputRef"
      accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
      class="file"
      type="file"
      @change="onChange"
    />
  </div>
</template>

<style scoped>
.nav-icon-field {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.preview {
  position: relative;
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}
.preview.clickable {
  cursor: pointer;
}
.preview.clickable:hover {
  border-color: #1677ff;
}
.preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.ph {
  font-size: 16px;
  color: #c0c4cc;
}
.mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #1677ff;
  background: rgb(255 255 255 / 75%);
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.side {
  flex: 1;
  min-width: 0;
}
.ops {
  display: flex;
  gap: 2px;
  margin-top: 2px;
}
.ops :deep(.el-button) {
  height: 20px;
  padding: 0 4px;
  font-size: 11px;
}
.file {
  display: none;
}
</style>
