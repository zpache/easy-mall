<script lang="ts" setup>
import { computed, h } from 'vue';

import { IconPicker } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { ElButton, ElInput } from 'element-plus';

import { NAV_ICONS } from '../icon-set';

defineOptions({ name: 'NavIconPickerField' });

const props = withDefaults(
  defineProps<{
    /** 当前图标：lucide:xxx 图标名，或历史数据里的图片地址 */
    modelValue?: string;
    /** 未选择时的占位图标 */
    placeholderIcon?: string;
  }>(),
  { modelValue: '', placeholderIcon: 'lucide:circle' },
);

const emit = defineEmits<{ 'update:modelValue': [value: string] }>();

const value = computed({
  get: () => props.modelValue ?? '',
  set: (val: string) => emit('update:modelValue', val ?? ''),
});

/** 历史数据可能是上传的图片地址，仍然按图片预览 */
const isImage = computed(() => /^(?:https?:)?\/\//.test(value.value));

/** IconPicker 需要一个输入框组件，这里复用 Element Plus 的 ElInput */
const inputComponent = h(ElInput, { size: 'small' });
</script>

<template>
  <div class="nav-icon-picker">
    <span class="preview" :class="{ empty: !value }">
      <img v-if="isImage" :src="value" alt="" />
      <IconifyIcon v-else :icon="value || placeholderIcon" class="ph" />
    </span>

    <IconPicker
      v-model="value"
      class="picker"
      :icons="NAV_ICONS"
      :input-component="inputComponent"
      :page-size="30"
      icon-slot="append"
      model-value-prop="model-value"
      prefix=""
    />

    <ElButton v-if="value" size="small" text @click="value = ''">清除</ElButton>
  </div>
</template>

<style scoped>
.nav-icon-picker {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
}
.preview {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  overflow: hidden;
  color: #1677ff;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}
.preview.empty {
  color: #c0c4cc;
}
.preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.ph {
  font-size: 16px;
}
.picker {
  flex: 1;
  min-width: 0;
}
</style>
