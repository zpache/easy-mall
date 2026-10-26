<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { goodsApi } from '@/api/goods'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'
import type { Goods } from '@/types/api'

const goods = ref<Goods | null>(null)
const goodsId = ref<number>(0)
const current = ref(0)

const cartStore = useCartStore()
const userStore = useUserStore()

const banners = computed(() => {
  if (!goods.value) return []
  const list = (goods.value.images || '').split(',').map((item) => item.trim()).filter(Boolean)
  return list.length ? list : [goods.value.mainImage]
})

async function load() {
  goods.value = await goodsApi.detail(goodsId.value)
  uni.setNavigationBarTitle({ title: goods.value.name })
}

async function requireLogin(): Promise<boolean> {
  if (userStore.isLogin) return true
  uni.navigateTo({ url: '/pages/login/index' })
  return false
}

async function addToCart() {
  if (!(await requireLogin())) return
  await cartStore.add(goodsId.value, 1)
  uni.showToast({ title: '已加入购物车', icon: 'none' })
}

async function buyNow() {
  if (!(await requireLogin())) return
  await cartStore.add(goodsId.value, 1)
  uni.navigateTo({ url: '/pages/order/confirm' })
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/index' })
}

onLoad((options) => {
  goodsId.value = Number(options?.id ?? 0)
  if (goodsId.value) {
    load()
  }
})
</script>

<template>
  <view v-if="goods" class="page">
    <swiper class="banner" :current="current" circular @change="(e) => (current = e.detail.current)">
      <swiper-item v-for="(image, index) in banners" :key="index">
        <image class="banner__image" :src="image" mode="aspectFill" />
      </swiper-item>
    </swiper>

    <view class="info">
      <view class="flex items-baseline">
        <text class="price-symbol price">¥</text>
        <text class="price text-48rpx">{{ formatPrice(goods.price) }}</text>
        <text v-if="goods.originalPrice" class="price-origin">¥{{ formatPrice(goods.originalPrice) }}</text>
      </view>
      <text class="info__name">{{ goods.name }}</text>
      <text v-if="goods.subtitle" class="info__subtitle">{{ goods.subtitle }}</text>
      <view class="info__meta">
        <text>库存 {{ goods.stock }}</text>
        <text>已售 {{ goods.sales || 0 }}</text>
        <text>{{ goods.unit || '件' }}</text>
      </view>
    </view>

    <view class="detail">
      <text class="detail__title">商品详情</text>
      <rich-text v-if="goods.detail" class="detail__content" :nodes="goods.detail" />
      <text v-else class="detail__empty">暂无详情</text>
    </view>

    <view class="bar safe-bottom">
      <view class="bar__item" @tap="goCart">
        <text class="bar__icon">🛒</text>
        <text class="bar__label">购物车</text>
      </view>
      <view class="bar__btn bar__btn--plain" @tap="addToCart">加入购物车</view>
      <view class="bar__btn bar__btn--primary" @tap="buyNow">立即购买</view>
    </view>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 140rpx;
}

.banner {
  width: 100%;
  height: 750rpx;
  background-color: #f2f2f2;

  &__image {
    width: 100%;
    height: 100%;
  }
}

.info {
  padding: 28rpx 24rpx;
  background-color: #fff;

  &__name {
    display: block;
    margin-top: 16rpx;
    font-size: 34rpx;
    font-weight: 600;
    line-height: 46rpx;
  }

  &__subtitle {
    display: block;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__meta {
    display: flex;
    gap: 32rpx;
    margin-top: 20rpx;
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.detail {
  margin-top: 20rpx;
  padding: 24rpx;
  background-color: #fff;

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__content {
    display: block;
    margin-top: 20rpx;
    font-size: 28rpx;
    line-height: 44rpx;
    color: #434343;
  }

  &__empty {
    display: block;
    margin-top: 20rpx;
    font-size: 26rpx;
    color: $text-placeholder;
  }
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background-color: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-right: 24rpx;
  }

  &__icon {
    font-size: 34rpx;
  }

  &__label {
    font-size: 20rpx;
    color: $text-secondary;
  }

  &__btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    border-radius: 44rpx;
    font-size: 28rpx;

    &--plain {
      background-color: $primary-light;
      color: $primary;
      margin-right: 16rpx;
    }

    &--primary {
      background-color: $primary;
      color: #fff;
    }
  }
}
</style>
