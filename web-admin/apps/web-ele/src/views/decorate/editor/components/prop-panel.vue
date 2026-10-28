<script lang="ts" setup>
import type { CanvasComponent, PageConfig } from '../../widgets/types';

import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { getWidget } from '../../widgets';
import PropField from './prop-field.vue';

const props = defineProps<{
  componentCount?: number;
  page: PageConfig;
  selected?: CanvasComponent;
}>();

const emit = defineEmits<{
  updatePage: [patch: Partial<PageConfig>];
  updateProps: [patch: Record<string, any>];
}>();

const definition = computed(() =>
  props.selected ? getWidget(props.selected.type) : null,
);

const pageFields = [
  { key: 'title', label: '页面标题', type: 'input' as const },
  { key: 'bgColor', label: '背景色', type: 'color' as const },
  { key: 'bgImage', label: '背景图', type: 'image' as const },
  { key: 'padding', label: '上下留白', max: 60, min: 0, type: 'number' as const },
];

function onPropUpdate(key: string, value: any) {
  emit('updateProps', { [key]: value });
}
</script>

<template>
  <aside class="prop-panel">
    <div class="panel-head">
      <span class="title">{{ selected ? `编辑：${definition?.name}` : '页面设置' }}</span>
      <span v-if="selected" class="component-type">{{ selected.type }}</span>
    </div>

    <div class="panel-body">
      <template v-if="selected && definition">
        <div class="hint">
          <IconifyIcon icon="lucide:mouse-pointer-click" class="mr-1" />
          修改即时生效，无需保存刷新
        </div>
        <PropField
          v-for="field in definition.fields"
          :key="field.key"
          :field="field"
          :model-value="selected.props[field.key]"
          @update:model-value="(val) => onPropUpdate(field.key, val)"
        />
      </template>

      <template v-else>
        <div class="hint">
          <IconifyIcon icon="lucide:info" class="mr-1" />
          点击画布中的组件可编辑其属性
        </div>
        <PropField
          v-for="field in pageFields"
          :key="field.key"
          :field="field as any"
          :model-value="page[field.key as keyof PageConfig]"
          @update:model-value="(val) => emit('updatePage', { [field.key]: val })"
        />
        <div class="page-meta">
          <p>当前页面共 {{ componentCount ?? 0 }} 个组件</p>
        </div>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.prop-panel {
  display: flex;
  flex-direction: column;
  width: 300px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #ebeef5;
}
.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f2f3f5;
}
.title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
}
.component-type {
  font-size: 10px;
  color: #a8abb2;
}
.panel-body {
  flex: 1;
  padding: 12px 14px 24px;
  overflow-y: auto;
}
.hint {
  padding: 6px 8px;
  margin-bottom: 12px;
  font-size: 11px;
  color: #1677ff;
  background: #f0f6ff;
  border-radius: 4px;
}
.page-meta {
  margin-top: 8px;
  font-size: 11px;
  color: #a8abb2;
}
</style>
