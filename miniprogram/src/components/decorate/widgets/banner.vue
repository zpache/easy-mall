<script setup lang="ts">
import { computed } from 'vue'
import { openLink, px } from '@/utils/decorate'

const props = defineProps<{ config: Record<string, any> }>()

const images = computed(() =>
  Array.isArray(props.config.images) && props.config.images.length > 0
    ? props.config.images
    : [{ url: '', link: '' }]
)

const interval = computed(() => Math.max(1, Number(props.config.interval) || 3) * 1000)
</script>

<template>
  <view class="d-banner" :style="{ padding: `${px(config.margin, 12)} 24rpx` }">
    <swiper
      class="d-banner__swiper"
      :style="{ height: px(config.height, 150), borderRadius: px(config.radius, 8) }"
      :autoplay="images.length > 1"
      :circular="images.length > 1"
      :interval="interval"
      :indicator-dots="images.length > 1"
      indicator-color="rgba(255,255,255,0.5)"
      indicator-active-color="#e02020"
    >
      <swiper-item v-for="(item, index) in images" :key="index">
        <image
          v-if="item.url"
          class="d-banner__img"
          :src="item.url"
          mode="aspectFill"
          @tap="openLink(item.link)"
        />
        <view v-else class="d-banner__placeholder">轮播图 {{ index + 1 }}</view>
      </swiper-item>
    </swiper>
  </view>
</template>

<style scoped lang="scss">
.d-banner {
  box-sizing: border-box;
}

.d-banner__swiper {
  overflow: hidden;
  transform: translateY(0);
}

.d-banner__img {
  display: block;
  width: 100%;
  height: 100%;
}

.d-banner__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 24rpx;
  color: #bbb;
  background-color: #eef0f2;
}
</style>
