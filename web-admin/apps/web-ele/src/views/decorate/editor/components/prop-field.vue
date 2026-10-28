<script lang="ts" setup>
import type { PropField } from '../../widgets/types';

import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElColorPicker,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElSwitch,
} from 'element-plus';

defineOptions({ name: 'PropField' });

const props = defineProps<{
  field: PropField;
  modelValue: any;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: any];
}>();

const value = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const items = computed<any[]>(() =>
  Array.isArray(props.modelValue) ? props.modelValue : [],
);

function updateItem(index: number, key: string, val: any) {
  const next = items.value.map((item, i) =>
    i === index ? { ...item, [key]: val } : item,
  );
  emit('update:modelValue', next);
}

function addItem() {
  const template: Record<string, any> = {};
  for (const sub of props.field.itemFields || []) {
    template[sub.key] = sub.type === 'number' ? 0 : '';
  }
  emit('update:modelValue', [...items.value, template]);
}

function removeItem(index: number) {
  emit(
    'update:modelValue',
    items.value.filter((_, i) => i !== index),
  );
}

function moveItem(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= items.value.length) {
    return;
  }
  const next = [...items.value];
  const [item] = next.splice(index, 1);
  next.splice(target, 0, item!);
  emit('update:modelValue', next);
}

function itemTitle(item: any, index: number) {
  const key = props.field.itemLabelKey;
  const text = key ? item?.[key] : '';
  return text ? String(text).slice(0, 18) : `第 ${index + 1} 项`;
}
</script>

<template>
  <div class="prop-field">
    <div class="field-label">
      <span>{{ field.label }}</span>
      <span v-if="field.tip" class="tip">{{ field.tip }}</span>
    </div>

    <!-- 基础类型 -->
    <ElInput
      v-if="field.type === 'input'"
      v-model="value"
      :placeholder="field.placeholder || '请输入'"
      size="small"
    />
    <ElInput
      v-else-if="field.type === 'textarea'"
      v-model="value"
      :placeholder="field.placeholder || '请输入'"
      :rows="4"
      size="small"
      type="textarea"
    />
    <ElInputNumber
      v-else-if="field.type === 'number'"
      v-model="value"
      :max="field.max"
      :min="field.min"
      :step="field.step ?? 1"
      class="w-full"
      controls-position="right"
      size="small"
    />
    <ElSwitch
      v-else-if="field.type === 'switch'"
      v-model="value"
      size="small"
    />
    <ElColorPicker
      v-else-if="field.type === 'color'"
      v-model="value"
      show-alpha
      size="small"
    />
    <ElSelect
      v-else-if="field.type === 'select'"
      v-model="value"
      class="w-full"
      size="small"
    >
      <ElOption
        v-for="option in field.options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </ElSelect>
    <div v-else-if="field.type === 'image'" class="image-field">
      <div class="image-preview">
        <img v-if="value" :src="value" alt="preview" />
        <IconifyIcon v-else class="placeholder-icon" icon="lucide:image" />
      </div>
      <div class="image-input">
        <ElInput v-model="value" placeholder="图片地址 https://…" size="small" />
      </div>
    </div>

    <!-- 列表类型 -->
    <div v-else-if="field.type === 'items'" class="items">
      <div v-for="(item, index) in items" :key="index" class="item">
        <div class="item-head">
          <span class="item-title">{{ itemTitle(item, index) }}</span>
          <span class="item-actions">
            <button title="上移" @click="moveItem(index, -1)">
              <IconifyIcon icon="lucide:chevron-up" />
            </button>
            <button title="下移" @click="moveItem(index, 1)">
              <IconifyIcon icon="lucide:chevron-down" />
            </button>
            <button class="danger" title="删除" @click="removeItem(index)">
              <IconifyIcon icon="lucide:trash-2" />
            </button>
          </span>
        </div>
        <div class="item-body">
          <PropField
            v-for="sub in field.itemFields"
            :key="sub.key"
            :field="sub"
            :model-value="item?.[sub.key]"
            @update:model-value="(val) => updateItem(index, sub.key, val)"
          />
        </div>
      </div>
      <ElButton
        :disabled="field.maxItems !== undefined && items.length >= field.maxItems"
        class="w-full"
        plain
        size="small"
        @click="addItem"
      >
        <IconifyIcon icon="lucide:plus" class="mr-1" />
        添加一项
      </ElButton>
    </div>
  </div>
</template>

<style scoped>
.prop-field {
  margin-bottom: 12px;
}
.field-label {
  display: flex;
  gap: 6px;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
  color: #606266;
}
.tip {
  font-size: 10px;
  color: #a8abb2;
}
.image-field {
  display: flex;
  gap: 8px;
  align-items: center;
}
.image-preview {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  overflow: hidden;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder-icon {
  font-size: 18px;
  color: #c0c4cc;
}
.image-input {
  flex: 1;
  min-width: 0;
}
.items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.item {
  padding: 6px 8px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.item-title {
  font-size: 11px;
  color: #909399;
}
.item-actions {
  display: flex;
  gap: 2px;
}
.item-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  font-size: 11px;
  color: #909399;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 3px;
}
.item-actions button:hover {
  color: #1677ff;
  background: #eef4ff;
}
.item-actions .danger:hover {
  color: #f56c6c;
  background: #fef0f0;
}
.item-body :deep(.prop-field:last-child) {
  margin-bottom: 0;
}
</style>
