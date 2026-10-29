<script lang="ts" setup>
import type { PropField } from '../../widgets/types';

import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElCheckbox,
  ElDialog,
  ElEmpty,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElPagination,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { getGoodsList } from '#/api/goods/goods';

defineOptions({ name: 'GoodsPicker' });

/**
 * 装修用的商品选择器。
 *
 * 存储的是商品快照（id / image / name / price / sales）而不是仅存 id：
 * 装修有「发布后才生效」的语义，快照保证已发布页面展示不随商品后续改价/改名漂移，
 * id 用于小程序端点击跳转商品详情。
 *
 * field.itemFields 可为每个已选商品追加编辑字段（如秒杀组件的秒杀价），
 * 结构与 items 类型的子字段一致（number / input）。
 */
const props = defineProps<{
  field?: PropField;
  maxItems?: number;
  modelValue: any;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: any];
}>();

const selected = computed<any[]>(() =>
  Array.isArray(props.modelValue) ? props.modelValue : [],
);

const extraFields = computed(() => props.field?.itemFields ?? []);

const limit = computed(() => props.maxItems ?? 10);

/* ===================== 选择弹窗 ===================== */

const visible = ref(false);
const keyword = ref('');
const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = 8;

/** 弹窗内临时选择，id -> 快照；翻页/搜索后保持 */
const draft = ref<Map<number, any>>(new Map());

function snapshot(row: any) {
  return {
    id: row.id,
    image: row.mainImage || '',
    name: row.name || '',
    price: Number(row.price ?? 0),
    sales: Number(row.sales ?? 0),
  };
}

async function load() {
  loading.value = true;
  try {
    const { items, total: count } = await getGoodsList({
      keyword: keyword.value.trim() || undefined,
      pageNum: pageNum.value,
      pageSize,
      status: 1,
    });
    rows.value = items || [];
    total.value = count || 0;
  } catch {
    rows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function open() {
  draft.value = new Map(selected.value.map((item) => [Number(item.id), item]));
  keyword.value = '';
  pageNum.value = 1;
  visible.value = true;
  load();
}

function isChecked(row: any) {
  return draft.value.has(Number(row.id));
}

function toggle(row: any, checked: boolean) {
  const next = new Map(draft.value);
  if (checked) {
    if (next.size >= limit.value && !next.has(Number(row.id))) {
      ElMessage.warning(`最多选择 ${limit.value} 个商品`);
      return;
    }
    // 已选过的商品保留其扩展字段（如已填的秒杀价），只刷新基础快照
    const existing = selected.value.find(
      (item) => Number(item.id) === Number(row.id),
    );
    next.set(
      Number(row.id),
      existing ? { ...existing, ...snapshot(row) } : snapshot(row),
    );
  } else {
    next.delete(Number(row.id));
  }
  draft.value = next;
}

function search() {
  pageNum.value = 1;
  load();
}

function changePage(page: number) {
  pageNum.value = page;
  load();
}

function confirm() {
  emit('update:modelValue', [...draft.value.values()]);
  visible.value = false;
}

/* ===================== 已选列表 ===================== */

function removeAt(index: number) {
  emit(
    'update:modelValue',
    selected.value.filter((_, i) => i !== index),
  );
}

function move(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= selected.value.length) {
    return;
  }
  const next = [...selected.value];
  const [item] = next.splice(index, 1);
  next.splice(target, 0, item!);
  emit('update:modelValue', next);
}

function updateExtra(index: number, key: string, val: any) {
  const next = selected.value.map((item, i) =>
    i === index ? { ...item, [key]: val } : item,
  );
  emit('update:modelValue', next);
}

function extraValue(item: any, sub: PropField) {
  if (sub.type === 'number') {
    const raw = item?.[sub.key];
    // 未填写保持输入框为空（undefined），避免误导为 0；小程序端会回退展示商品原价
    return raw === undefined || raw === null ? undefined : Number(raw);
  }
  return item?.[sub.key];
}
</script>

<template>
  <div class="goods-picker">
    <!-- 已选商品 -->
    <div v-if="selected.length > 0" class="picked">
      <div v-for="(item, index) in selected" :key="item.id" class="picked-item">
        <img v-if="item.image" :src="item.image" alt="" class="picked-cover" />
        <div
          v-else
          class="picked-cover picked-cover--empty"
        >
          <IconifyIcon icon="lucide:image" />
        </div>
        <div class="picked-info">
          <p class="picked-name" :title="item.name">{{ item.name }}</p>
          <div class="picked-extras">
            <span class="picked-price">¥{{ Number(item.price ?? 0) }}</span>
            <label
              v-for="sub in extraFields"
              :key="sub.key"
              class="picked-extra"
            >
              <span>{{ sub.label }}</span>
              <ElInputNumber
                v-if="sub.type === 'number'"
                :max="sub.max"
                :min="sub.min"
                :model-value="extraValue(item, sub)"
                :step="sub.step ?? 1"
                controls-position="right"
                size="small"
                @update:model-value="(val: any) => updateExtra(index, sub.key, val)"
              />
              <ElInput
                v-else
                :model-value="extraValue(item, sub) ?? ''"
                size="small"
                @update:model-value="(val: any) => updateExtra(index, sub.key, val)"
              />
            </label>
          </div>
        </div>
        <span class="picked-actions">
          <button :disabled="index === 0" title="上移" @click="move(index, -1)">
            <IconifyIcon icon="lucide:chevron-up" />
          </button>
          <button
            :disabled="index === selected.length - 1"
            title="下移"
            @click="move(index, 1)"
          >
            <IconifyIcon icon="lucide:chevron-down" />
          </button>
          <button class="danger" title="移除" @click="removeAt(index)">
            <IconifyIcon icon="lucide:x" />
          </button>
        </span>
      </div>
    </div>

    <ElButton
      :disabled="selected.length >= limit"
      class="w-full"
      dashed
      size="small"
      @click="open"
    >
      <IconifyIcon class="mr-1" icon="lucide:package-plus" />
      {{ selected.length > 0 ? '继续添加' : '选择商品' }}
      <span v-if="selected.length > 0">（{{ selected.length }}/{{ limit }}）</span>
    </ElButton>

    <ElDialog
      v-model="visible"
      append-to-body
      title="选择商品"
      width="640px"
    >
      <div class="picker-toolbar">
        <ElInput
          v-model="keyword"
          clearable
          placeholder="搜索商品名称"
          size="small"
          @change="search"
          @clear="search"
        >
          <template #prefix>
            <IconifyIcon icon="lucide:search" />
          </template>
        </ElInput>
      </div>
      <ElTable
        v-loading="loading"
        :data="rows"
        height="360px"
        size="small"
      >
        <ElTableColumn width="42">
          <template #default="{ row }">
            <ElCheckbox
              :model-value="isChecked(row)"
              @change="(val: any) => toggle(row, val === true)"
            />
          </template>
        </ElTableColumn>
        <ElTableColumn label="商品" min-width="240">
          <template #default="{ row }">
            <div class="picker-goods">
              <img v-if="row.mainImage" :src="row.mainImage" alt="" />
              <div v-else class="picker-goods__empty">
                <IconifyIcon icon="lucide:image" />
              </div>
              <div class="picker-goods__meta">
                <p class="picker-goods__name">{{ row.name }}</p>
                <span class="picker-goods__price">
                  ¥{{ Number(row.price ?? 0) }}
                  <em>库存 {{ row.stock ?? 0 }}</em>
                </span>
              </div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="分类" prop="categoryName" width="110" />
        <ElTableColumn label="状态" width="70">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'on' : 'off'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </span>
          </template>
        </ElTableColumn>
      </ElTable>
      <div class="picker-footer">
        <span class="picker-count">已选 {{ draft.size }} / {{ limit }}</span>
        <ElPagination
          :current-page="pageNum"
          :page-size="pageSize"
          :pager-count="5"
          :total="total"
          background
          layout="prev, pager, next"
          size="small"
          @current-change="changePage"
        />
      </div>
      <template #footer>
        <ElButton size="small" @click="visible = false">取消</ElButton>
        <ElButton size="small" type="primary" @click="confirm">确定</ElButton>
      </template>
      <ElEmpty
        v-show="!loading && rows.length === 0"
        description="没有匹配的商品"
      />
    </ElDialog>
  </div>
</template>

<style scoped>
.picked {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 6px;
}
.picked-item {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 6px 8px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.picked-cover {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: #c0c4cc;
  font-size: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  object-fit: cover;
}
.picked-cover--empty {
  border: 1px dashed #dcdfe6;
}
.picked-info {
  flex: 1;
  min-width: 0;
}
.picked-name {
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  color: #303133;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picked-extras {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 2px;
}
.picked-price {
  font-size: 11px;
  font-weight: 600;
  color: #e02020;
}
.picked-extra {
  display: flex;
  gap: 4px;
  align-items: center;
  font-size: 10px;
  color: #909399;
}
.picked-extra :deep(.el-input-number) {
  width: 90px;
}
.picked-extra :deep(.el-input) {
  width: 90px;
}
.picked-actions {
  display: flex;
  flex: none;
  gap: 2px;
}
.picked-actions button {
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
.picked-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}
.picked-actions button:hover:not(:disabled) {
  color: #1677ff;
  background: #eef4ff;
}
.picked-actions .danger:hover:not(:disabled) {
  color: #f56c6c;
  background: #fef0f0;
}
.picker-toolbar {
  margin-bottom: 8px;
}
.picker-goods {
  display: flex;
  gap: 8px;
  align-items: center;
}
.picker-goods img {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}
.picker-goods__empty {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: #c0c4cc;
  background: #f5f7fa;
  border-radius: 4px;
}
.picker-goods__meta {
  min-width: 0;
}
.picker-goods__name {
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  color: #303133;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picker-goods__price {
  font-size: 11px;
  color: #e02020;
}
.picker-goods__price em {
  font-style: normal;
  color: #a8abb2;
}
.picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.picker-count {
  font-size: 12px;
  color: #909399;
}
.on {
  font-size: 11px;
  color: #67c23a;
}
.off {
  font-size: 11px;
  color: #a8abb2;
}
</style>
