<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div class="widget-search" :style="{ padding: `${config.margin ?? 10}px 12px` }">
    <div
      class="search-bar"
      :style="{
        background: config.bgColor || '#ffffff',
        borderRadius: `${config.radius ?? 18}px`,
        justifyContent: config.align === 'center' ? 'center' : 'flex-start',
      }"
    >
      <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2">
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.5-3.5" />
      </svg>
      <span class="placeholder">{{ config.placeholder || '搜索商品' }}</span>
    </div>
  </div>
</template>

<style scoped>
.widget-search {
  box-sizing: border-box;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
}
.icon {
  width: 15px;
  height: 15px;
  flex: none;
}
.placeholder {
  overflow: hidden;
  font-size: 12px;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
