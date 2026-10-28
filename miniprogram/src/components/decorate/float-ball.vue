<script setup lang="ts">
import { computed } from 'vue'
import type { FloatConfig } from '@/types/decorate'
import { FLOAT_GLYPHS, switchTab } from '@/utils/decorate'

const props = withDefaults(
  defineProps<{
    config: FloatConfig
    /** 距底部距离（rpx），页面自行避开 TabBar */
    bottom?: number
  }>(),
  { bottom: 140 }
)

const items = computed(() =>
  props.config.enabled ? props.config.itemList.filter((item) => item.enabled) : []
)

const positionStyle = computed(() => ({
  bottom: `${props.bottom}rpx`,
  [props.config.position === 'left' ? 'left' : 'right']: '20rpx'
}))

function onTap(key: string) {
  const item = props.config.itemList.find((it) => it.key === key)
  if (!item) return
  if (key === 'service') {
    if (item.value) {
      uni.makePhoneCall({ phoneNumber: item.value, fail: () => undefined })
    } else {
      uni.showToast({ title: '客服电话未配置', icon: 'none' })
    }
    return
  }
  if (key === 'cart') {
    switchTab('/pages/cart/index')
    return
  }
  if (key === 'top') {
    uni.pageScrollTo({ scrollTop: 0, duration: 240 })
  }
}
</script>

<template>
  <view v-if="items.length" class="d-float" :style="positionStyle">
    <view v-for="item in items" :key="item.key" class="d-float__ball" @tap="onTap(item.key)">
      <image v-if="item.icon" class="d-float__img" :src="item.icon" mode="aspectFit" />
      <text v-else class="d-float__glyph">{{ FLOAT_GLYPHS[item.key] || '•' }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-float {
  position: fixed;
  z-index: 95;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.d-float__ball {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  background-color: rgba(255, 255, 255, 0.94);
  border-radius: 50%;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.14);
}

.d-float__img {
  width: 40rpx;
  height: 40rpx;
}

.d-float__glyph {
  font-size: 32rpx;
  line-height: 1;
  color: #606266;
}
</style>
