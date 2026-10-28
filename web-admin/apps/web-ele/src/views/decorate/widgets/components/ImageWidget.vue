<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div class="widget-image" :style="{ padding: `${config.margin ?? 12}px 12px` }">
    <img
      v-if="config.url"
      :src="config.url"
      alt="ad"
      :style="{ borderRadius: `${config.radius ?? 8}px`, height: config.height ? `${config.height}px` : 'auto' }"
    />
    <div
      v-else
      class="placeholder"
      :style="{ borderRadius: `${config.radius ?? 8}px`, height: `${config.height ?? 120}px` }"
    >
      单图广告
    </div>
  </div>
</template>

<style scoped>
.widget-image {
  box-sizing: border-box;
}
img {
  display: block;
  width: 100%;
  object-fit: cover;
}
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #bbb;
  background: #eef0f2;
}
</style>
