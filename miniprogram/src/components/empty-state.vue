<script setup lang="ts">
withDefaults(
  defineProps<{
    text?: string
    tip?: string
  }>(),
  { text: '暂无数据', tip: '' }
)
</script>

<template>
  <view class="empty-state">
    <view class="empty-state__icon">🗂</view>
    <text class="empty-state__text">{{ text }}</text>
    <text v-if="tip" class="empty-state__tip">{{ tip }}</text>
    <slot />
  </view>
</template>

<style scoped lang="scss">

.empty-state {
  padding: 120rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__icon {
    font-size: 80rpx;
    opacity: 0.4;
  }

  &__text {
    margin-top: 20rpx;
    font-size: 28rpx;
    color: $text-secondary;
  }

  &__tip {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: $text-placeholder;
  }
}
</style>
