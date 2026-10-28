<script lang="ts" setup>
import type { CanvasComponent } from '../../widgets/types';

import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { getWidget } from '../../widgets';

const props = defineProps<{
  component: CanvasComponent;
  index: number;
  selected: boolean;
}>();

const emit = defineEmits<{
  duplicate: [id: string];
  dragstart: [index: number];
  move: [id: string, offset: number];
  remove: [id: string];
  select: [id: string];
  toggle: [id: string];
}>();

const definition = computed(() => getWidget(props.component.type));
</script>

<template>
  <div
    class="canvas-item"
    :class="{ selected, hidden: !component.visible }"
    draggable="true"
    @click.stop="emit('select', component.id)"
    @dragstart.stop="emit('dragstart', index)"
  >
    <div class="label">
      <span class="name">{{ definition?.name }}</span>
      <span v-if="!component.visible" class="hidden-tag">已隐藏</span>
    </div>

    <div class="toolbar" @click.stop>
      <button title="上移" @click="emit('move', component.id, -1)">
        <IconifyIcon icon="lucide:chevron-up" />
      </button>
      <button title="下移" @click="emit('move', component.id, 1)">
        <IconifyIcon icon="lucide:chevron-down" />
      </button>
      <button title="复制" @click="emit('duplicate', component.id)">
        <IconifyIcon icon="lucide:copy" />
      </button>
      <button
        :title="component.visible ? '隐藏' : '显示'"
        @click="emit('toggle', component.id)"
      >
        <IconifyIcon :icon="component.visible ? 'lucide:eye' : 'lucide:eye-off'" />
      </button>
      <button class="danger" title="删除" @click="emit('remove', component.id)">
        <IconifyIcon icon="lucide:trash-2" />
      </button>
    </div>

    <div class="render">
      <component
        :is="definition?.component"
        v-if="definition"
        :config="component.props"
      />
    </div>
  </div>
</template>

<style scoped>
.canvas-item {
  position: relative;
  border: 1px dashed transparent;
  transition: border-color 0.15s;
}
.canvas-item:hover {
  border-color: #90b8ff;
}
.canvas-item.selected {
  border: 2px solid #1677ff;
}
.canvas-item.hidden .render {
  opacity: 0.35;
  filter: grayscale(1);
}
.label {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 3;
  display: none;
  gap: 4px;
  align-items: center;
  padding: 1px 6px;
  font-size: 10px;
  color: #fff;
  background: #1677ff;
  border-radius: 0 0 4px;
}
.canvas-item.selected .label {
  display: flex;
}
.hidden-tag {
  padding: 0 3px;
  color: #ffd666;
  border-left: 1px solid rgb(255 255 255 / 35%);
}
.toolbar {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 3;
  display: none;
  background: #1677ff;
  border-radius: 0 0 0 4px;
}
.canvas-item:hover .toolbar,
.canvas-item.selected .toolbar {
  display: flex;
}
.toolbar button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 20px;
  padding: 0;
  font-size: 12px;
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: none;
}
.toolbar button:hover {
  background: rgb(255 255 255 / 20%);
}
.toolbar .danger:hover {
  background: #f56c6c;
}
.render {
  pointer-events: none;
}
</style>
