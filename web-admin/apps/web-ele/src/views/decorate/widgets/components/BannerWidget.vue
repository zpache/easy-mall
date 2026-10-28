<script lang="ts" setup>
import { computed } from 'vue';

const props = defineProps<{ config: Record<string, any> }>();

const images = computed(() =>
  Array.isArray(props.config.images) && props.config.images.length > 0
    ? props.config.images
    : [{ url: '', link: '' }],
);
const radius = computed(() => `${props.config.radius ?? 8}px`);
</script>

<template>
  <div class="widget-banner" :style="{ padding: `${config.margin ?? 12}px 12px` }">
    <div class="track" :style="{ borderRadius: radius }">
      <div
        v-for="(item, index) in images"
        :key="index"
        class="slide"
        :style="{ height: `${config.height ?? 150}px` }"
      >
        <img v-if="item.url" :src="item.url" :alt="`banner-${index}`" />
        <div v-else class="placeholder">轮播图 {{ index + 1 }}</div>
      </div>
    </div>
    <div v-if="images.length > 1" class="dots">
      <span v-for="(_, index) in images" :key="index" class="dot" :class="{ active: index === 0 }" />
    </div>
  </div>
</template>

<style scoped>
.widget-banner {
  box-sizing: border-box;
}
.track {
  display: flex;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
}
.track::-webkit-scrollbar {
  display: none;
}
.slide {
  flex: 0 0 100%;
  scroll-snap-align: start;
  overflow: hidden;
  background: #eef0f2;
}
.slide img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 12px;
  color: #bbb;
}
.dots {
  display: flex;
  gap: 4px;
  justify-content: center;
  margin-top: 6px;
}
.dot {
  width: 5px;
  height: 5px;
  background: rgb(0 0 0 / 15%);
  border-radius: 50%;
}
.dot.active {
  width: 12px;
  background: #e02020;
  border-radius: 3px;
}
</style>
