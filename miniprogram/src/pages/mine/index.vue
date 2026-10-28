<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import { useDecorateStore } from '@/store/decorate'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const decorateStore = useDecorateStore()

const nickname = computed(() => userStore.profile?.nickname || '未登录')
const avatar = computed(() => userStore.profile?.avatar || '')

async function ensureLogin() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function toLogin() {
  if (userStore.isLogin) {
    return
  }
  uni.navigateTo({ url: '/pages/login/index' })
}

async function toOrders(status?: number) {
  if (!(await ensureLogin())) return
  const query = status === undefined ? '' : `?status=${status}`
  uni.navigateTo({ url: `/pages/order/list${query}` })
}

async function toAddress() {
  if (!(await ensureLogin())) return
  uni.navigateTo({ url: '/pages/address/list' })
}

async function logout() {
  const res = await uni.showModal({ title: '提示', content: '确定退出登录吗？' })
  if (res.confirm) {
    userStore.logout()
    uni.showToast({ title: '已退出', icon: 'none' })
  }
}

onShow(() => {
  if (userStore.isLogin) {
    userStore.fetchProfile().catch(() => userStore.logout())
  }
  decorateStore.ensureNav()
})
</script>

<template>
  <view class="page">
    <view class="profile">
      <image v-if="avatar" class="profile__avatar" :src="avatar" mode="aspectFill" />
      <view v-else class="profile__avatar profile__avatar--placeholder">{{ nickname.slice(0, 1) }}</view>
      <view class="profile__info">
        <text class="profile__name">{{ nickname }}</text>
        <text class="profile__desc" @tap="toLogin">
          {{ userStore.isLogin ? '欢迎回来，买点好东西' : '点击登录，开启购物之旅' }}
        </text>
      </view>
    </view>

    <view class="card">
      <view class="card__head">
        <text class="card__title">我的订单</text>
        <text class="card__more" @tap="toOrders()">全部订单 ›</text>
      </view>
      <view class="order-entries">
        <view class="order-entries__item" @tap="toOrders(0)">
          <text class="order-entries__icon">💰</text>
          <text class="order-entries__label">待付款</text>
        </view>
        <view class="order-entries__item" @tap="toOrders(1)">
          <text class="order-entries__icon">📦</text>
          <text class="order-entries__label">待发货</text>
        </view>
        <view class="order-entries__item" @tap="toOrders(2)">
          <text class="order-entries__icon">🚚</text>
          <text class="order-entries__label">待收货</text>
        </view>
        <view class="order-entries__item" @tap="toOrders(3)">
          <text class="order-entries__icon">✅</text>
          <text class="order-entries__label">已完成</text>
        </view>
      </view>
    </view>

    <view class="card card--menu">
      <view class="cell cell-arrow" @tap="toAddress">
        <text>收货地址</text>
      </view>
      <view class="cell cell-arrow" @tap="toOrders(0)">
        <text>待付款订单</text>
      </view>
      <view class="cell">
        <text>关于 easy-mall</text>
        <text class="text-24rpx text-light">v1.0.0</text>
      </view>
    </view>

    <view v-if="userStore.isLogin" class="logout" @tap="logout">退出登录</view>

    <DecorateTabbar :config="decorateStore.nav.tabbar" current="/pages/mine/index" />
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 150rpx;
}

.profile {
  display: flex;
  align-items: center;
  padding: 48rpx 32rpx 56rpx;
  background: linear-gradient(135deg, #ff8a4c 0%, $primary 100%);

  &__avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    background-color: rgba(255, 255, 255, 0.3);
    border: 4rpx solid rgba(255, 255, 255, 0.6);

    &--placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 44rpx;
      font-weight: 600;
    }
  }

  &__info {
    margin-left: 24rpx;
  }

  &__name {
    display: block;
    color: #fff;
    font-size: 36rpx;
    font-weight: 600;
  }

  &__desc {
    display: block;
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
  }
}

.card {
  margin: 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;

  &--menu {
    padding: 0 24rpx;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__more {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

.order-entries {
  display: flex;
  margin-top: 28rpx;

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__icon {
    font-size: 40rpx;
  }

  &__label {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: $text-secondary;
  }
}

.logout {
  margin: 32rpx 24rpx;
  padding: 26rpx 0;
  text-align: center;
  background-color: #fff;
  border-radius: 20rpx;
  color: $danger;
  font-size: 28rpx;
}
</style>
