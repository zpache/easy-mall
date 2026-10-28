<script setup lang="ts">
import { computed } from 'vue'
import { openLink } from '@/utils/decorate'

const props = defineProps<{ config: Record<string, any> }>()

const items = computed(() => (Array.isArray(props.config.items) ? props.config.items : []))
const columns = computed(() => Number(props.config.columns) || 5)

const cellStyle = computed(() => ({
  width: `${100 / columns.value}%`
}))
</script>

<template>
  <view class="d-nav-grid" :style="{ background: config.bgColor || '#ffffff' }">
    <view
      v-for="(item, index) in items"
      :key="index"
      class="d-nav-grid__cell"
      :style="cellStyle"
      @tap="openLink(item.link)"
    >
      <image v-if="item.icon" class="d-nav-grid__icon" :src="item.icon" mode="aspectFill" />
      <view v-else class="d-nav-grid__icon d-nav-grid__icon--empty" />
      <text class="d-nav-grid__text">{{ item.text }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-nav-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 24rpx 8rpx;
}

.d-nav-grid__cell {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  align-items: center;
  box-sizing: border-box;
  padding-bottom: 12rpx;
}

.d-nav-grid__icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
}

.d-nav-grid__icon--empty {
  background-color: #eef0f2;
}

.d-nav-grid__text {
  font-size: 22rpx;
  color: #333;
}
</style>
