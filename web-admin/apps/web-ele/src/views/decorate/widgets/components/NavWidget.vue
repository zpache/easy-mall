<script lang="ts" setup>
import { computed } from 'vue';

const props = defineProps<{ config: Record<string, any> }>();

const items = computed(() => (Array.isArray(props.config.items) ? props.config.items : []));
const columns = computed(() => props.config.columns ?? 5);
</script>

<template>
  <div class="widget-nav" :style="{ background: config.bgColor || '#ffffff' }">
    <div class="grid" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
      <div v-for="(item, index) in items" :key="index" class="cell">
        <img v-if="item.icon" class="icon" :src="item.icon" :alt="item.text" />
        <div v-else class="icon placeholder" />
        <span class="text">{{ item.text }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.widget-nav {
  padding: 12px 4px;
}
.grid {
  display: grid;
  gap: 10px 0;
}
.cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: center;
}
.icon {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 50%;
}
.placeholder {
  background: #eef0f2;
}
.text {
  font-size: 11px;
  color: #333;
}
</style>
