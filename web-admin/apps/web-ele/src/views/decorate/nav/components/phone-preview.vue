<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import type { DecorateNavApi } from '#/api/decorate/nav';

import NavFloatBall from './nav-float-ball.vue';
import NavNavbar from './nav-navbar.vue';
import NavTabbar from './nav-tabbar.vue';

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

const immersive = computed(() => props.navbar.style === 'immersive');
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
      <NavNavbar :navbar="navbar" />

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
      <NavFloatBall :bottom="66" :float-ball="floatBall" />

      <!-- 底部 TabBar -->
      <NavTabbar :tabbar="tabbar" />
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
  flex: none;
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
</style>
