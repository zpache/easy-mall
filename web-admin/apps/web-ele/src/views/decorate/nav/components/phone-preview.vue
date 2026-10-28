<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import type { DecorateNavApi } from '#/api/decorate/nav';

defineOptions({ name: 'NavPhonePreview' });

const props = withDefaults(
  defineProps<{
    floatBall: DecorateNavApi.FloatBall;
    navbar: DecorateNavApi.Navbar;
    pageName?: string;
    tabbar: DecorateNavApi.Tabbar;
  }>(),
  { pageName: '商城首页' },
);

const TAB_ICONS: Record<string, string> = {
  cart: 'lucide:shopping-cart',
  category: 'lucide:grid-2x2',
  home: 'lucide:home',
  mine: 'lucide:user',
};

const FLOAT_ICONS: Record<string, string> = {
  cart: 'lucide:shopping-cart',
  service: 'lucide:headset',
  top: 'lucide:arrow-up',
};

const tabs = computed(() =>
  props.tabbar.items.filter((item) => item.visible !== false),
);

const floatItems = computed(() =>
  props.floatBall.enabled
    ? props.floatBall.itemList.filter((item) => item.enabled)
    : [],
);

const immersive = computed(() => props.navbar.style === 'immersive');
const showBack = computed(
  () => props.navbar.showBack && props.navbar.backBehavior !== 'none',
);

function tabIcon(item: DecorateNavApi.TabItem, index: number) {
  const selected = index === 0;
  const custom = selected ? item.selectedIcon || item.icon : item.icon;
  return custom || TAB_ICONS[item.key] || 'lucide:circle';
}

function borderStyle(type: string) {
  if (type === 'none') {
    return 'none';
  }
  return `1px solid ${type === 'white' ? '#ffffff' : 'rgb(0 0 0 / 8%)'}`;
}
</script>

<template>
  <div class="phone">
    <div class="screen">
      <!-- 状态栏 -->
      <div class="status-bar" :class="{ light: immersive }">
        <span class="time">9:41</span>
        <span class="signal">
          <IconifyIcon icon="lucide:signal" />
          <IconifyIcon icon="lucide:wifi" />
          <IconifyIcon icon="lucide:battery-medium" />
        </span>
      </div>

      <!-- 顶部导航 -->
      <div
        class="navbar"
        :class="{ immersive }"
        :style="{
          background: immersive ? 'transparent' : navbar.bgColor || '#ffffff',
          color: immersive ? '#ffffff' : navbar.textColor || '#333333',
        }"
      >
        <span v-if="showBack" class="back">
          <IconifyIcon icon="lucide:chevron-left" />
        </span>
        <span class="title">{{ navbar.title || '商城' }}</span>
        <span class="placeholder" />
      </div>

      <!-- 内容占位 -->
      <div class="content" :class="{ behind: immersive }">
        <div class="banner">
          <span>{{ pageName }}</span>
        </div>
        <div class="grid">
          <div v-for="i in 4" :key="i" class="card">
            <div class="thumb" />
            <div class="line" />
            <div class="line short" />
          </div>
        </div>
        <div class="hint">页面内容由「页面装修」决定，此处仅预览导航</div>
      </div>

      <!-- 悬浮快捷入口 -->
      <div
        v-if="floatItems.length > 0"
        class="float-ball"
        :class="floatBall.position"
      >
        <div v-for="item in floatItems" :key="item.key" class="ball" :title="item.name">
          <img v-if="item.icon" :src="item.icon" alt="" />
          <IconifyIcon v-else :icon="FLOAT_ICONS[item.key] || 'lucide:circle'" />
        </div>
      </div>

      <!-- 底部 TabBar -->
      <div
        v-if="tabbar.enabled"
        class="tabbar"
        :style="{
          background: tabbar.bgColor || '#ffffff',
          borderTop: borderStyle(tabbar.borderStyle),
        }"
      >
        <div
          v-for="(item, index) in tabs"
          :key="item.key"
          class="tab"
          :style="{ color: index === 0 ? tabbar.selectedColor : tabbar.color }"
        >
          <img
            v-if="(index === 0 ? item.selectedIcon || item.icon : item.icon)"
            class="tab-icon-img"
            :src="index === 0 ? item.selectedIcon || item.icon : item.icon"
            alt=""
          />
          <IconifyIcon v-else class="tab-icon" :icon="tabIcon(item, index)" />
          <span class="tab-name">{{ item.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.phone {
  display: flex;
  justify-content: center;
}
.screen {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 320px;
  height: 640px;
  overflow: hidden;
  background: #f5f6f7;
  border: 8px solid #1f2329;
  border-radius: 34px;
  box-shadow: 0 12px 32px rgb(0 0 0 / 18%);
}
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 26px;
  padding: 0 14px;
  font-size: 11px;
  color: #333;
}
.status-bar.light {
  color: #fff;
  text-shadow: 0 1px 2px rgb(0 0 0 / 35%);
}
.signal {
  display: flex;
  gap: 3px;
  font-size: 12px;
}
.navbar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 10px;
  font-size: 15px;
  font-weight: 600;
}
.navbar.immersive {
  margin-bottom: -44px;
  text-shadow: 0 1px 3px rgb(0 0 0 / 45%);
}
.back,
.placeholder {
  display: flex;
  align-items: center;
  width: 22px;
  font-size: 18px;
}
.placeholder {
  width: 22px;
}
.title {
  flex: 1;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.content {
  position: relative;
  flex: 1;
  padding: 8px;
  overflow: hidden;
}
.content.behind {
  padding-top: 52px;
}
.banner {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 96px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #4a8cff, #7a5cff);
  border-radius: 10px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.card {
  padding: 6px;
  background: #fff;
  border-radius: 8px;
}
.thumb {
  height: 62px;
  background: #eef1f5;
  border-radius: 6px;
}
.line {
  height: 7px;
  margin-top: 6px;
  background: #f0f2f5;
  border-radius: 4px;
}
.line.short {
  width: 55%;
}
.hint {
  margin-top: 10px;
  font-size: 10px;
  color: #a8abb2;
  text-align: center;
}
.float-ball {
  position: absolute;
  right: 10px;
  bottom: 74px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.float-ball.left {
  right: auto;
  left: 10px;
}
.ball {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  font-size: 15px;
  color: #606266;
  background: rgb(255 255 255 / 92%);
  border: 1px solid rgb(0 0 0 / 6%);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgb(0 0 0 / 12%);
}
.ball img {
  width: 18px;
  height: 18px;
  object-fit: contain;
}
.tabbar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 52px;
  padding-bottom: 4px;
}
.tab {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}
.tab-icon {
  font-size: 19px;
}
.tab-icon-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}
.tab-name {
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
