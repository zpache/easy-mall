<script lang="ts" setup>
import type { DecorateNavApi as NavApi } from '#/api/decorate/nav';

import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { FLOAT_ICONS, isIconImage } from '../config';

defineOptions({ name: 'NavFloatBall' });

const props = withDefaults(
  defineProps<{
    /** 距底部距离（px） */
    bottom?: number;
    /** 跟随视口固定（全屏预览用），否则相对父容器绝对定位 */
    fixed?: boolean;
    floatBall: NavApi.FloatBall;
  }>(),
  { bottom: 72, fixed: false },
);

const items = computed(() =>
  props.floatBall?.enabled
    ? (props.floatBall.itemList ?? []).filter((item) => item.enabled)
    : [],
);
</script>

<template>
  <div
    v-if="items.length > 0"
    class="nav-float"
    :class="[floatBall.position, { fixed }]"
    :style="{ bottom: `${bottom}px` }"
  >
    <div
      v-for="item in items"
      :key="item.key"
      class="nav-ball"
      :title="item.name"
    >
      <img v-if="isIconImage(item.icon)" :src="item.icon" alt="" />
      <IconifyIcon
        v-else
        :icon="item.icon || FLOAT_ICONS[item.key] || 'lucide:circle'"
      />
    </div>
  </div>
</template>

<style scoped>
.nav-float {
  position: absolute;
  right: 10px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.nav-float.left {
  right: auto;
  left: 10px;
}
/* 全屏预览：手机宽度居中显示时贴着内容边缘固定 */
.nav-float.fixed {
  position: fixed;
}
.nav-float.fixed.right {
  right: max(12px, calc(50% - 228px));
}
.nav-float.fixed.left {
  left: max(12px, calc(50% - 228px));
}
.nav-ball {
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
.nav-ball img {
  width: 18px;
  height: 18px;
  object-fit: contain;
}
</style>
