<script lang="ts" setup>
import type { DecorateNavApi as NavApi } from '#/api/decorate/nav';

import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

defineOptions({ name: 'NavNavbar' });

const props = withDefaults(
  defineProps<{
    navbar: NavApi.Navbar;
    /** 常规样式下吸顶（全屏预览用）；沉浸式始终叠加在内容上 */
    sticky?: boolean;
  }>(),
  { sticky: false },
);

const immersive = computed(() => props.navbar?.style === 'immersive');
const showBack = computed(
  () => props.navbar?.showBack && props.navbar?.backBehavior !== 'none',
);
const sticky = computed(() => props.sticky && !immersive.value);
</script>

<template>
  <div
    class="nav-navbar"
    :class="{ immersive, sticky }"
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
</template>

<style scoped>
.nav-navbar {
  position: relative;
  z-index: 2;
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 10px;
  font-size: 15px;
  font-weight: 600;
}
.nav-navbar.sticky {
  position: sticky;
  top: 0;
}
.nav-navbar.immersive {
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
.title {
  flex: 1;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
