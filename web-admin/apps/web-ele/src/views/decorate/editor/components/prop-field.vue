<script lang="ts" setup>
import type { PropField } from '../../widgets/types';

import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElColorPicker,
  ElDatePicker,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElSwitch,
} from 'element-plus';

import CouponPicker from './coupon-picker.vue';
import GoodsPicker from './goods-picker.vue';
import MaterialPicker from './material-picker.vue';

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

/**
 * 图片统一从「素材中心」取：装修里不再各自直传，
 * 上传动作收敛到素材选择器内部（上传即登记素材库），避免同一张图重复上传。
 */
const materialPickerVisible = ref(false);
/** 轮播图等列表字段的批量选择 */
const batchPickerVisible = ref(false);

function openMaterialPicker() {
  materialPickerVisible.value = true;
}

function clearImage() {
  emit('update:modelValue', '');
}

function onMaterialSelect(url: string) {
  emit('update:modelValue', url);
}

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

/** 列表字段里的图片子字段（有则支持一次从素材库批量添加多行） */
const imageSubField = computed(() =>
  (props.field.itemFields || []).find((sub) => sub.type === 'image'),
);

function onBatchConfirm(urls: string[]) {
  const key = imageSubField.value?.key;
  if (!key) {
    return;
  }
  const limit = props.field.maxItems;
  const next = items.value.map((item) => ({ ...item }));
  for (const url of urls) {
    if (limit !== undefined && next.length >= limit) {
      break;
    }
    const template: Record<string, any> = {};
    for (const sub of props.field.itemFields || []) {
      template[sub.key] = sub.type === 'number' ? 0 : '';
    }
    template[key] = url;
    next.push(template);
  }
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
    <ElDatePicker
      v-else-if="field.type === 'datetime'"
      :model-value="value || ''"
      class="w-full"
      format="YYYY-MM-DD HH:mm:ss"
      placeholder="选择日期时间"
      size="small"
      type="datetime"
      value-format="YYYY-MM-DD HH:mm:ss"
      @update:model-value="(val: any) => (value = val || '')"
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
    <CouponPicker
      v-else-if="field.type === 'coupons'"
      :max="field.max"
      :model-value="value"
      @update:model-value="(val: any) => (value = val)"
    />
    <GoodsPicker
      v-else-if="field.type === 'goods'"
      :field="field"
      :max-items="field.maxItems"
      :model-value="value"
      @update:model-value="(val: any) => (value = val)"
    />
    <div v-else-if="field.type === 'image'" class="image-field">
      <div
        class="image-preview clickable"
        title="从素材中心选择图片"
        @click="openMaterialPicker"
      >
        <img v-if="value" :src="value" alt="preview" />
        <IconifyIcon v-else class="placeholder-icon" icon="lucide:image-plus" />
      </div>
      <div class="image-input">
        <ElInput
          v-model="value"
          placeholder="图片地址 https://…（建议从素材中心选择）"
          size="small"
        />
        <div class="image-actions">
          <ElButton size="small" text type="primary" @click="openMaterialPicker">
            从素材中心选择
          </ElButton>
          <ElButton v-if="value" size="small" text @click="clearImage">
            清除
          </ElButton>
        </div>
      </div>
      <MaterialPicker
        v-model="materialPickerVisible"
        :selected="value ? [value] : []"
        @select="onMaterialSelect"
      />
    </div>

    <!-- 列表类型 -->
    <div v-else-if="field.type === 'items'" class="items">
      <ElButton
        v-if="imageSubField"
        class="w-full"
        plain
        size="small"
        @click="batchPickerVisible = true"
      >
        <IconifyIcon icon="lucide:images" class="mr-1" />
        从素材中心批量添加
      </ElButton>
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
      <MaterialPicker
        v-if="imageSubField"
        v-model="batchPickerVisible"
        :limit="
          field.maxItems ? Math.max(field.maxItems - items.length, 0) : 0
        "
        multiple
        title="批量选择图片"
        @confirm="onBatchConfirm"
      />
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
  align-items: flex-start;
}
.image-preview {
  position: relative;
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
.image-preview.clickable {
  cursor: pointer;
}
.image-preview.clickable:hover {
  border-color: #1677ff;
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
.image-actions {
  display: flex;
  gap: 2px;
  align-items: center;
  margin-top: 2px;
}
.image-actions :deep(.el-button) {
  height: 20px;
  padding: 0 4px;
  font-size: 11px;
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
