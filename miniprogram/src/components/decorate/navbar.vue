<script setup lang="ts">
import { computed } from 'vue'
import type { NavbarConfig } from '@/types/decorate'
import { statusBarHeight, switchTab } from '@/utils/decorate'

const props = withDefaults(
  defineProps<{
    config: NavbarConfig
    /** 覆盖标题（微页面用页面名） */
    title?: string
    /** 是否展示返回按钮（首页不展示） */
    showBack?: boolean
  }>(),
  { title: '', showBack: false }
)

const barHeight = 44
const statusPx = statusBarHeight()

const immersive = computed(() => props.config.style === 'immersive')
const text = computed(() => props.title || props.config.title || '易商城')

const style = computed(() => {
  const base: Record<string, string> = {
    paddingTop: `${statusPx}px`,
    color: immersive.value ? '#ffffff' : props.config.textColor || '#333333',
    background: immersive.value ? 'transparent' : props.config.bgColor || '#ffffff'
  }
  return base
})

function onBack() {
  const behavior = props.config.backBehavior
  if (behavior === 'none') return
  if (behavior === 'home') {
    switchTab('/pages/index/index')
    return
  }
  uni.navigateBack({
    fail: () => switchTab('/pages/index/index')
  })
}
</script>

<template>
  <view class="d-navbar" :class="{ 'd-navbar--immersive': immersive }" :style="style">
    <view class="d-navbar__bar" :style="{ height: `${barHeight}px` }">
      <view class="d-navbar__side">
        <text v-if="showBack && config.showBack" class="d-navbar__back" @tap="onBack">‹</text>
      </view>
      <text class="d-navbar__title">{{ text }}</text>
      <view class="d-navbar__side" />
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-navbar {
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 100;
}

.d-navbar__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
}

.d-navbar__side {
  display: flex;
  align-items: center;
  width: 60rpx;
}

.d-navbar__back {
  font-size: 52rpx;
  line-height: 1;
}

.d-navbar__title {
  flex: 1;
  overflow: hidden;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.d-navbar--immersive .d-navbar__title,
.d-navbar--immersive .d-navbar__back {
  text-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.45);
}
</style>
