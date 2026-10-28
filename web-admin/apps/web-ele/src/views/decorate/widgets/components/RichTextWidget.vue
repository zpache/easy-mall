<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div
    class="widget-rich-text"
    :style="{
      padding: `${config.margin ?? 12}px 12px`,
      color: config.color || '#333333',
      fontSize: `${config.size ?? 13}px`,
      background: config.bgColor || 'transparent',
    }"
    v-html="config.content || '<p>在此填写富文本内容</p>'"
  />
</template>

<style scoped>
.widget-rich-text {
  box-sizing: border-box;
  line-height: 1.7;
  overflow-wrap: break-word;
}
.widget-rich-text :deep(img) {
  max-width: 100%;
}
.widget-rich-text :deep(p) {
  margin: 0 0 6px;
}
</style>
