<script lang="ts" setup>
import type { PageSchema } from '../../widgets/types';

import { ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import CanvasItem from './canvas-item.vue';

const props = defineProps<{
  schema: PageSchema;
  selectedId: null | string;
}>();

const emit = defineEmits<{
  clear: [];
  duplicate: [id: string];
  drop: [index: number];
  dragend: [];
  dragstart: [index: number];
  move: [id: string, offset: number];
  remove: [id: string];
  select: [id: string];
  toggle: [id: string];
}>();

const hoverIndex = ref<null | number>(null);

function onDragOver(event: DragEvent, index: number) {
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
  hoverIndex.value = index;
}

function onDrop(index: number) {
  hoverIndex.value = null;
  emit('drop', index);
}
</script>

<template>
  <div class="canvas-stage" @click="emit('clear')">
    <div class="phone">
      <div class="status-bar">
        <span class="time">9:41</span>
        <span class="status-icons">
          <IconifyIcon icon="lucide:signal" />
          <IconifyIcon icon="lucide:wifi" />
          <IconifyIcon icon="lucide:battery-full" />
        </span>
      </div>
      <div
        class="screen"
        :style="{
          background: schema.page.bgImage
            ? `url(${schema.page.bgImage}) center / cover no-repeat`
            : schema.page.bgColor,
          padding: `${schema.page.padding ?? 0}px 0`,
        }"
      >
        <div
          v-for="(item, index) in schema.components"
          :key="item.id"
          class="slot-wrap"
        >
          <div
            class="drop-slot"
            :class="{ active: hoverIndex === index }"
            @dragover="onDragOver($event, index)"
            @dragleave="hoverIndex = null"
            @drop.stop.prevent="onDrop(index)"
          >
            <span v-if="hoverIndex === index" class="drop-hint">插入到这里</span>
          </div>
          <CanvasItem
            :component="item"
            :index="index"
            :selected="selectedId === item.id"
            @duplicate="emit('duplicate', $event)"
            @dragstart="emit('dragstart', $event)"
            @move="(id, offset) => emit('move', id, offset)"
            @remove="emit('remove', $event)"
            @select="emit('select', $event)"
            @toggle="emit('toggle', $event)"
          />
        </div>

        <div
          class="drop-slot tail"
          :class="{ active: hoverIndex === schema.components.length }"
          @dragover="onDragOver($event, schema.components.length)"
          @dragleave="hoverIndex = null"
          @drop.stop.prevent="onDrop(schema.components.length)"
        >
          <span v-if="hoverIndex === schema.components.length" class="drop-hint">
            插入到末尾
          </span>
        </div>

        <div v-if="schema.components.length === 0" class="empty">
          <IconifyIcon class="empty-icon" icon="lucide:layout-template" />
          <p class="empty-title">从左侧拖入组件开始装修</p>
          <p class="empty-tip">支持上下插入、拖动排序、点击选中</p>
        </div>
      </div>
      <div class="home-bar" />
    </div>
  </div>
</template>

<style scoped>
.canvas-stage {
  display: flex;
  flex: 1;
  justify-content: center;
  height: 100%;
  padding: 20px 0;
  overflow-y: auto;
  background: #eef0f3;
}
.phone {
  display: flex;
  flex-direction: column;
  width: 375px;
  height: 720px;
  overflow: hidden;
  background: #fff;
  border: 8px solid #1f2329;
  border-radius: 32px;
  box-shadow: 0 12px 32px rgb(0 0 0 / 18%);
}
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 26px;
  padding: 0 16px;
  font-size: 11px;
  color: #1f2329;
  background: #fff;
}
.status-icons {
  display: flex;
  gap: 4px;
  font-size: 11px;
}
.screen {
  position: relative;
  flex: 1;
  overflow-y: auto;
  scrollbar-width: thin;
}
.slot-wrap {
  position: relative;
}
.drop-slot {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 2px;
  background: transparent;
  transition: height 0.12s;
}
.drop-slot.active {
  height: 34px;
  background: rgb(22 119 255 / 12%);
  border-top: 2px solid #1677ff;
  border-bottom: 2px solid #1677ff;
}
.drop-slot.tail {
  height: 16px;
}
.drop-slot.tail.active {
  height: 40px;
}
.drop-hint {
  font-size: 11px;
  color: #1677ff;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 90px 20px;
  color: #c0c4cc;
}
.empty-icon {
  margin-bottom: 10px;
  font-size: 34px;
}
.empty-title {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.empty-tip {
  margin: 4px 0 0;
  font-size: 11px;
  color: #c0c4cc;
}
.home-bar {
  height: 5px;
  background: #fff;
}
.home-bar::after {
  display: block;
  width: 120px;
  height: 4px;
  margin: 0 auto;
  content: '';
  background: #d9d9d9;
  border-radius: 2px;
}
</style>
