<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ config: Record<string, any> }>()

const goods = computed(() => (Array.isArray(props.config.goods) ? props.config.goods : []))
const isGrid = computed(() => props.config.layout !== 'list')
const columns = computed(() => Number(props.config.columns) || 2)

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${columns.value}, 1fr)`
}))

function toDetail(id: number | string) {
  if (!id) return
  uni.navigateTo({ url: `/pages/goods/detail?id=${id}` })
}
</script>

<template>
  <view class="d-goods" :style="{ background: config.bgColor || 'transparent' }">
    <view v-if="isGrid" class="d-goods__grid" :style="gridStyle">
      <view v-for="item in goods" :key="item.id" class="d-goods__card" @tap="toDetail(item.id)">
        <image class="d-goods__cover" :src="item.image" mode="aspectFill" />
        <view class="d-goods__info">
          <text class="d-goods__name">{{ item.name }}</text>
          <view class="d-goods__bottom">
            <text class="d-goods__price"><text class="d-goods__yuan">¥</text>{{ item.price }}</text>
            <text v-if="config.showSales" class="d-goods__sales">已售 {{ item.sales }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="d-goods__list">
      <view v-for="item in goods" :key="item.id" class="d-goods__row" @tap="toDetail(item.id)">
        <image class="d-goods__thumb" :src="item.image" mode="aspectFill" />
        <view class="d-goods__info">
          <text class="d-goods__name d-goods__name--row">{{ item.name }}</text>
          <view class="d-goods__bottom">
            <text class="d-goods__price"><text class="d-goods__yuan">¥</text>{{ item.price }}</text>
            <text v-if="config.showSales" class="d-goods__sales">已售 {{ item.sales }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-goods {
  padding: 16rpx 20rpx;
}

.d-goods__grid {
  display: grid;
  gap: 16rpx;
}

.d-goods__card {
  overflow: hidden;
  background-color: #fff;
  border-radius: 16rpx;
}

.d-goods__cover {
  display: block;
  width: 100%;
  height: 320rpx;
}

.d-goods__info {
  flex: 1;
  min-width: 0;
  padding: 14rpx 16rpx 16rpx;
}

.d-goods__name {
  display: -webkit-box;
  overflow: hidden;
  font-size: 24rpx;
  line-height: 1.35;
  color: #333;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.d-goods__name--row {
  font-size: 26rpx;
}

.d-goods__bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 12rpx;
}

.d-goods__price {
  font-size: 30rpx;
  font-weight: 600;
  color: #e02020;
}

.d-goods__yuan {
  font-size: 20rpx;
}

.d-goods__sales {
  font-size: 20rpx;
  color: #999;
}

.d-goods__list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.d-goods__row {
  display: flex;
  gap: 20rpx;
  padding: 16rpx;
  background-color: #fff;
  border-radius: 16rpx;
}

.d-goods__thumb {
  flex: none;
  width: 168rpx;
  height: 168rpx;
  border-radius: 12rpx;
}
</style>
