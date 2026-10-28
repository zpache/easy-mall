<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div
    class="widget-title"
    :style="{
      textAlign: config.align || 'left',
      background: config.bgColor || 'transparent',
      padding: `${config.margin ?? 12}px 12px`,
    }"
  >
    <div class="row" :style="{ justifyContent: config.align === 'center' ? 'center' : 'space-between' }">
      <div class="texts">
        <span
          class="text"
          :style="{
            fontSize: `${config.size ?? 16}px`,
            color: config.color || '#333333',
            fontWeight: config.bold ? 600 : 400,
          }"
        >{{ config.text || '标题' }}</span>
        <span v-if="config.subtitle" class="subtitle">{{ config.subtitle }}</span>
      </div>
      <span v-if="config.more && config.align !== 'center'" class="more">更多 ›</span>
    </div>
  </div>
</template>

<style scoped>
.widget-title {
  box-sizing: border-box;
}
.row {
  display: flex;
  align-items: center;
}
.texts {
  display: flex;
  gap: 6px;
  align-items: baseline;
}
.text {
  line-height: 1.3;
}
.subtitle {
  font-size: 11px;
  color: #999;
}
.more {
  font-size: 11px;
  color: #999;
}
</style>
