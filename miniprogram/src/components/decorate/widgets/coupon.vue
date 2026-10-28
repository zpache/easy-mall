<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ config: Record<string, any> }>()

const coupons = computed(() => (Array.isArray(props.config.coupons) ? props.config.coupons : []))

function onClaim() {
  // TODO 接入营销中心领券接口后改为真实领取
  uni.showToast({ title: '优惠券活动暂未开放领取', icon: 'none' })
}

function condOf(item: Record<string, any>) {
  return Number(item.threshold) > 0 ? `满${item.threshold}可用` : '无门槛'
}
</script>

<template>
  <view class="d-coupon" :style="{ background: config.bgColor || '#ffffff' }">
    <view class="d-coupon__head">
      <text class="d-coupon__title">{{ config.title || '领券中心' }}</text>
      <text class="d-coupon__more">更多 ›</text>
    </view>
    <view class="d-coupon__list">
      <view v-for="(item, index) in coupons" :key="index" class="d-coupon__card">
        <view class="d-coupon__left">
          <text v-if="item.discount" class="d-coupon__amount">
            {{ (item.discount / 10).toFixed(1) }}<text class="d-coupon__unit">折</text>
          </text>
          <text v-else class="d-coupon__amount">
            <text class="d-coupon__unit">¥</text>{{ item.amount }}
          </text>
          <text class="d-coupon__cond">{{ condOf(item) }}</text>
        </view>
        <view class="d-coupon__mid">
          <text class="d-coupon__name">{{ item.name }}</text>
          <text class="d-coupon__valid">{{ item.validDays }} 天内有效</text>
        </view>
        <text class="d-coupon__btn" @tap="onClaim">领取</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-coupon {
  padding: 24rpx;
}

.d-coupon__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.d-coupon__title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.d-coupon__more {
  font-size: 22rpx;
  color: #999;
}

.d-coupon__list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.d-coupon__card {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 16rpx 20rpx;
  background: linear-gradient(90deg, #fff5f5 0%, #ffffff 60%);
  border: 1rpx solid #ffe0e0;
  border-radius: 16rpx;
}

.d-coupon__left {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 124rpx;
  padding-right: 16rpx;
  border-right: 1rpx dashed #ffd0d0;
}

.d-coupon__amount {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1;
  color: #e02020;
}

.d-coupon__unit {
  font-size: 22rpx;
  font-weight: 400;
}

.d-coupon__cond {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #e02020;
}

.d-coupon__mid {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.d-coupon__name {
  overflow: hidden;
  font-size: 24rpx;
  color: #333;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.d-coupon__valid {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #999;
}

.d-coupon__btn {
  flex: none;
  padding: 8rpx 20rpx;
  font-size: 22rpx;
  color: #fff;
  background-color: #e02020;
  border-radius: 24rpx;
}
</style>
