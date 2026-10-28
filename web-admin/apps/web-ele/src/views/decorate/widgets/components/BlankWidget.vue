<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div
    class="widget-blank"
    :style="{
      height: `${config.height ?? 12}px`,
      background: config.bgColor || 'transparent',
    }"
  />
</template>

<style scoped>
.widget-blank {
  width: 100%;
}
</style>
