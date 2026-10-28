<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div
    class="widget-notice"
    :style="{
      background: config.bgColor || '#fff7e8',
      color: config.color || '#ff6b00',
    }"
  >
    <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M11 5 6 9H3v6h3l5 4V5Z" />
      <path d="M16 9a4 4 0 0 1 0 6" />
    </svg>
    <span class="text">{{ config.text || '公告内容' }}</span>
  </div>
</template>

<style scoped>
.widget-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  overflow: hidden;
  font-size: 12px;
}
.icon {
  width: 14px;
  height: 14px;
  flex: none;
}
.text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
