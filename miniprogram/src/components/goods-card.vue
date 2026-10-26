<script setup lang="ts">
import type { Goods } from '@/types/api'
import { formatPrice } from '@/utils/format'

const props = defineProps<{
  goods: Goods
  /** 列表模式：横向排列 */
  horizontal?: boolean
}>()

function toDetail() {
  uni.navigateTo({ url: `/pages/goods/detail?id=${props.goods.id}` })
}
</script>

<template>
  <view
    class="goods-card"
    :class="horizontal ? 'goods-card--row' : ''"
    @tap="toDetail"
  >
    <image class="goods-card__image" :src="goods.mainImage" mode="aspectFill" lazy-load />
    <view class="goods-card__body">
      <text class="goods-card__name ellipsis-2">{{ goods.name }}</text>
      <text v-if="goods.subtitle" class="goods-card__subtitle ellipsis">{{ goods.subtitle }}</text>
      <view class="goods-card__bottom">
        <view class="flex items-baseline">
          <text class="price-symbol price">¥</text>
          <text class="price text-32rpx">{{ formatPrice(goods.price) }}</text>
          <text v-if="goods.originalPrice" class="price-origin">¥{{ formatPrice(goods.originalPrice) }}</text>
        </view>
        <text class="goods-card__sales">已售 {{ goods.sales || 0 }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">

.goods-card {
  width: 100%;
  background-color: #fff;
  border-radius: 16rpx;
  overflow: hidden;

  &__image {
    width: 100%;
    height: 340rpx;
    background-color: #f2f2f2;
    display: block;
  }

  &__body {
    padding: 16rpx 20rpx 24rpx;
  }

  &__name {
    font-size: 28rpx;
    line-height: 38rpx;
    color: $text-main;
    font-weight: 500;
  }

  &__subtitle {
    display: block;
    margin-top: 6rpx;
    font-size: 24rpx;
    color: $text-secondary;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 14rpx;
  }

  &__sales {
    font-size: 22rpx;
    color: $text-placeholder;
  }

  &--row {
    display: flex;
    padding: 20rpx 0;

    .goods-card__image {
      width: 180rpx;
      height: 180rpx;
      border-radius: 12rpx;
      flex-shrink: 0;
    }

    .goods-card__body {
      flex: 1;
      min-width: 0;
      padding: 4rpx 0 0 20rpx;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
  }
}
</style>
