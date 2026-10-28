<script setup lang="ts">
import { computed } from 'vue'
import type { TabbarConfig } from '@/types/decorate'
import { isTabActive, resolveTabTarget, switchTab, TAB_GLYPHS } from '@/utils/decorate'

const props = withDefaults(
  defineProps<{
    config: TabbarConfig
    /** 当前页面路由，如 /pages/index/index */
    current: string
    /** 当前微页面 ID（渲染 /pages/decorate/index 时用） */
    pageId?: string
  }>(),
  { pageId: '' }
)

const items = computed(() =>
  props.config.enabled ? props.config.items.filter((item) => item.visible !== false) : []
)

const activeKey = computed(
  () => items.value.find((item) => isTabActive(item, props.current, props.pageId))?.key || ''
)

function tabOf(key: string) {
  return props.config.items.find((item) => item.key === key)
}

function iconOf(key: string) {
  const item = tabOf(key)
  if (!item) return ''
  return key === activeKey.value ? item.selectedIcon || item.icon : item.icon
}

function onTap(key: string) {
  const item = tabOf(key)
  if (!item) return
  const target = resolveTabTarget(item)
  if (!target) return
  const sameRoute = props.current === target.route
  if (sameRoute && (!target.pageId || String(target.pageId) === String(props.pageId))) return
  if (target.tab) {
    switchTab(target.url)
  } else {
    uni.navigateTo({ url: target.url })
  }
}

const borderTop = computed(() => {
  if (props.config.borderStyle === 'none') return 'none'
  return `1rpx solid ${props.config.borderStyle === 'white' ? '#ffffff' : 'rgba(0,0,0,0.08)'}`
})
</script>

<template>
  <view
    v-if="items.length"
    class="d-tabbar"
    :style="{ background: config.bgColor || '#ffffff', borderTop }"
  >
    <view
      v-for="item in items"
      :key="item.key"
      class="d-tabbar__item"
      :style="{ color: item.key === activeKey ? config.selectedColor : config.color }"
      @tap="onTap(item.key)"
    >
      <image
        v-if="iconOf(item.key)"
        class="d-tabbar__img"
        :src="iconOf(item.key)"
        mode="aspectFit"
      />
      <text v-else class="d-tabbar__glyph">{{ TAB_GLYPHS[item.key] || '•' }}</text>
      <text class="d-tabbar__text">{{ item.name }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-tabbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 104rpx;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

.d-tabbar__item {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4rpx;
  align-items: center;
  justify-content: center;
  height: 104rpx;
}

.d-tabbar__img {
  width: 40rpx;
  height: 40rpx;
}

.d-tabbar__glyph {
  font-size: 34rpx;
  line-height: 40rpx;
}

.d-tabbar__text {
  max-width: 120rpx;
  overflow: hidden;
  font-size: 20rpx;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
