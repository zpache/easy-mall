<script setup lang="ts">
defineProps<{ config: Record<string, any> }>()

function callService(phone?: string) {
  if (!phone) {
    uni.showToast({ title: '客服电话未配置', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber: phone, fail: () => undefined })
}
</script>

<template>
  <view class="d-shop" :style="{ background: config.bgColor || '#ffffff' }">
    <image v-if="config.logo" class="d-shop__logo" :src="config.logo" mode="aspectFill" />
    <view class="d-shop__info">
      <text class="d-shop__name">{{ config.name || '店铺名称' }}</text>
      <text v-if="config.desc" class="d-shop__desc">{{ config.desc }}</text>
    </view>
    <text class="d-shop__btn" @tap="callService(config.phone)">联系客服</text>
  </view>
</template>

<style scoped lang="scss">
.d-shop {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 24rpx;
}

.d-shop__logo {
  flex: none;
  width: 88rpx;
  height: 88rpx;
  border-radius: 20rpx;
}

.d-shop__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.d-shop__name {
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
}

.d-shop__desc {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #999;
}

.d-shop__btn {
  flex: none;
  padding: 10rpx 20rpx;
  font-size: 22rpx;
  color: #ff6b00;
  border: 1rpx solid #ffd9bf;
  border-radius: 24rpx;
}
</style>
