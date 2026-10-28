<script lang="ts" setup>
import type { DecorateNavApi as NavApi } from '#/api/decorate/nav';

import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { isIconImage, TAB_ICONS, tabbarBorder } from '../config';

defineOptions({ name: 'NavTabbar' });

const props = withDefaults(
  defineProps<{
    /** 选中态下标，默认首个标签 */
    activeIndex?: number;
    tabbar: NavApi.Tabbar;
  }>(),
  { activeIndex: 0 },
);

const tabs = computed(() =>
  (props.tabbar?.items ?? []).filter((item) => item.visible !== false),
);

function active(index: number) {
  return index === props.activeIndex;
}

function imageOf(item: NavApi.TabItem, isActive: boolean) {
  return (isActive ? item.selectedIcon || item.icon : item.icon) || '';
}
</script>

<template>
  <div
    v-if="tabbar.enabled"
    class="nav-tabbar"
    :style="{
      background: tabbar.bgColor || '#ffffff',
      borderTop: tabbarBorder(tabbar.borderStyle),
    }"
  >
    <div
      v-for="(item, index) in tabs"
      :key="item.key"
      class="nav-tab"
      :style="{
        color: active(index) ? tabbar.selectedColor : tabbar.color,
      }"
    >
      <img
        v-if="isIconImage(imageOf(item, active(index)))"
        :src="imageOf(item, active(index))"
        alt=""
        class="nav-tab-img"
      />
      <IconifyIcon
        v-else
        class="nav-tab-icon"
        :icon="imageOf(item, active(index)) || TAB_ICONS[item.key] || 'lucide:circle'"
      />
      <span class="nav-tab-name">{{ item.name }}</span>
    </div>
  </div>
</template>

<style scoped>
.nav-tabbar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-around;
  height: 52px;
  padding-bottom: 4px;
}
.nav-tab {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}
.nav-tab-icon {
  font-size: 19px;
}
.nav-tab-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}
.nav-tab-name {
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
