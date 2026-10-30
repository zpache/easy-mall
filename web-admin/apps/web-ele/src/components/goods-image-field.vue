<script lang="ts" setup>
import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import MaterialPicker from '#/views/decorate/editor/components/material-picker.vue';

defineOptions({ name: 'GoodsImageField' });

/**
 * 商品图片字段：主图 / 轮播图统一从素材中心选取，不再手填 URL。
 * - 单选模式：点缩略图区域打开选择器，选中即回填；
 * - 多选模式：可加多张（max 上限），支持逐张移除，值以英文逗号拼接（与后端存储一致）。
 */
const props = withDefaults(
  defineProps<{
    modelValue?: string;
    /** 多选（轮播图） */
    multiple?: boolean;
    /** 多选上限，0 表示不限制 */
    max?: number;
  }>(),
  {
    max: 0,
    modelValue: '',
    multiple: false,
  },
);

const emit = defineEmits<{ 'update:modelValue': [value: string] }>();

const pickerVisible = ref(false);

const urls = computed(() =>
  String(props.modelValue || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean),
);

const reachedMax = computed(
  () => props.multiple && props.max > 0 && urls.value.length >= props.max,
);

function openPicker() {
  if (reachedMax.value) {
    return;
  }
  pickerVisible.value = true;
}

/** 单选：选中即回填 */
function onSelect(url: string) {
  emit('update:modelValue', url);
}

/** 多选：确认后整体替换（选择器里已带入当前已选，可增可删） */
function onConfirm(picked: string[]) {
  emit('update:modelValue', picked.join(','));
}

function onRemove(index: number) {
  const next = [...urls.value];
  next.splice(index, 1);
  emit('update:modelValue', props.multiple ? next.join(',') : '');
}
</script>

<template>
  <div class="goods-image-field">
    <div class="goods-image-field__thumbs">
      <div
        v-for="(url, index) in urls"
        :key="url"
        class="goods-image-field__thumb"
      >
        <img :src="url" alt="" loading="lazy" />
        <button
          class="goods-image-field__remove"
          title="移除"
          type="button"
          @click="onRemove(index)"
        >
          <IconifyIcon icon="lucide:x" />
        </button>
      </div>

      <button
        class="goods-image-field__add"
        :class="{ 'goods-image-field__add--disabled': reachedMax }"
        :title="reachedMax ? `最多 ${max} 张` : '从素材中心选择'"
        type="button"
        @click="openPicker"
      >
        <IconifyIcon icon="lucide:image-plus" />
        <span>{{ multiple ? '添加图片' : '选择图片' }}</span>
      </button>
    </div>

    <div v-if="multiple && max > 0" class="goods-image-field__hint">
      {{ urls.length }} / {{ max }} 张
    </div>

    <MaterialPicker
      v-model="pickerVisible"
      :limit="multiple ? max : 0"
      :multiple="multiple"
      :selected="urls"
      :title="multiple ? '从素材中心选择轮播图' : '从素材中心选择商品主图'"
      :upload-scene="'goods'"
      @confirm="onConfirm"
      @select="onSelect"
    />
  </div>
</template>

<style scoped>
.goods-image-field__thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.goods-image-field__thumb {
  position: relative;
  width: 76px;
  height: 76px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.goods-image-field__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-image-field__remove {
  position: absolute;
  top: 2px;
  right: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  color: #fff;
  cursor: pointer;
  background: rgb(0 0 0 / 55%);
  border: none;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.15s;
}

.goods-image-field__thumb:hover .goods-image-field__remove {
  opacity: 1;
}

.goods-image-field__add {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  justify-content: center;
  width: 76px;
  height: 76px;
  font-size: 11px;
  color: var(--foreground);
  cursor: pointer;
  background: var(--accent);
  border: 1px dashed hsl(var(--border));
  border-radius: 6px;
  transition: border-color 0.15s;
}

.goods-image-field__add:hover {
  border-color: var(--primary);
}

.goods-image-field__add--disabled {
  color: hsl(var(--muted-foreground));
  cursor: not-allowed;
  opacity: 0.6;
}

.goods-image-field__hint {
  margin-top: 4px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}
</style>
