<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { CATEGORY_LABELS, WIDGETS } from '../../widgets';

const emit = defineEmits<{
  add: [type: string];
  dragend: [];
  dragstart: [type: string];
}>();

const groups = computed(() => {
  const order: string[] = ['base', 'content', 'goods', 'marketing'];
  return order
    .map((category) => ({
      category,
      label: CATEGORY_LABELS[category as keyof typeof CATEGORY_LABELS],
      widgets: WIDGETS.filter((item) => item.category === category),
    }))
    .filter((group) => group.widgets.length > 0);
});

function onDragStart(event: DragEvent, type: string) {
  event.dataTransfer?.setData('text/widget-type', type);
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy';
  }
  emit('dragstart', type);
}
</script>

<template>
  <aside class="widget-panel">
    <div class="panel-head">
      <span class="title">组件库</span>
      <span class="tip">拖入画布或点击添加</span>
    </div>
    <div class="panel-body">
      <div v-for="group in groups" :key="group.category" class="group">
        <div class="group-title">{{ group.label }}</div>
        <div class="grid">
          <div
            v-for="widget in group.widgets"
            :key="widget.type"
            class="widget"
            draggable="true"
            @dragend="emit('dragend')"
            @dragstart="onDragStart($event, widget.type)"
            @click="emit('add', widget.type)"
          >
            <IconifyIcon :icon="widget.icon" class="icon" />
            <span class="name">{{ widget.name }}</span>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.widget-panel {
  display: flex;
  flex-direction: column;
  width: 232px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #ebeef5;
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
.tip {
  font-size: 11px;
  color: #a8abb2;
}
.panel-body {
  flex: 1;
  padding: 8px 12px 16px;
  overflow-y: auto;
}
.group {
  margin-top: 10px;
}
.group-title {
  margin-bottom: 6px;
  font-size: 11px;
  color: #909399;
}
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}
.widget {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  padding: 8px 2px;
  cursor: grab;
  background: #f7f8fa;
  border: 1px solid transparent;
  border-radius: 6px;
  transition: all 0.15s;
}
.widget:hover {
  color: #1677ff;
  background: #eef4ff;
  border-color: #b9d3ff;
}
.widget:active {
  cursor: grabbing;
}
.icon {
  font-size: 17px;
}
.name {
  font-size: 10px;
  line-height: 1.2;
  color: #606266;
  text-align: center;
}
</style>
