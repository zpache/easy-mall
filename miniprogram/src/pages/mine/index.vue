<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import { couponApi } from '@/api/coupon'
import { useDecorateStore } from '@/store/decorate'
import { useUserStore } from '@/store/user'
import { resolveMediaUrl } from '@/utils/url'
import { formatPrice } from '@/utils/format'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const userStore = useUserStore()
const decorateStore = useDecorateStore()

const nickname = computed(() => userStore.profile?.nickname || '未登录')
const avatar = computed(() => resolveMediaUrl(userStore.profile?.avatar))
/** 未设置头像时用昵称首字占位 */
const avatarText = computed(() => (userStore.isLogin ? nickname.value.slice(0, 1) : '未'))
const profileDesc = computed(() => {
  if (!userStore.isLogin) return '点击登录，开启购物之旅'
  return userStore.profileIncomplete ? '微信不再下发头像昵称，点这里设置 ›' : '欢迎回来，买点好东西'
})

/** 会员等级文案：带折扣率，如「银卡会员 9.8折」 */
const levelText = computed(() => {
  const profile = userStore.profile
  if (!profile?.levelName) return '普通会员'
  const rate = Number(profile.levelDiscount ?? 100)
  const text = rate > 0 && rate < 100 ? `${(rate / 10).toFixed(rate % 10 === 0 ? 0 : 1)}折` : ''
  return text ? `${profile.levelName} ${text}` : profile.levelName
})

/** 会员资产：等级 / 积分 / 累计消费，数据随订单支付由服务端结算 */
const memberStats = computed(() => {
  const profile = userStore.profile
  return [
    { label: '会员等级', value: levelText.value },
    { label: '可用积分', value: String(profile?.points ?? 0) },
    { label: '累计消费', value: `¥${formatPrice(profile?.totalSpend ?? 0)}` }
  ]
})

/** 可用优惠券张数：个人中心入口上的角标文案 */
const couponAvailable = ref(0)
const couponText = computed(() => {
  if (!userStore.isLogin) return '登录后查看可用券'
  return couponAvailable.value > 0 ? `${couponAvailable.value} 张可用` : '暂无可用券'
})

async function loadCouponCount() {
  if (!userStore.isLogin) {
    couponAvailable.value = 0
    return
  }
  try {
    const res = await couponApi.mine()
    couponAvailable.value = res.available
  } catch {
    // 券数量属于辅助信息，失败不打断个人中心
  }
}

async function ensureLogin() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function toProfile() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/mine/profile' })
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

async function toCoupons() {
  if (!(await ensureLogin())) return
  uni.navigateTo({ url: '/pages/coupon/index' })
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
    loadCouponCount()
  }
  decorateStore.ensureNav()
  decorateStore.ensureTheme()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page">
    <view class="profile" @tap="toProfile">
      <image v-if="avatar" class="profile__avatar" :src="avatar" mode="aspectFill" />
      <view v-else class="profile__avatar profile__avatar--placeholder">{{ avatarText }}</view>
      <view class="profile__info">
        <text class="profile__name">{{ nickname }}</text>
        <text class="profile__desc">{{ profileDesc }}</text>
      </view>
      <text v-if="userStore.isLogin" class="profile__arrow">›</text>
    </view>

    <view v-if="userStore.isLogin" class="card stats">
      <view v-for="item in memberStats" :key="item.label" class="stats__item">
        <text class="stats__value">{{ item.value }}</text>
        <text class="stats__label">{{ item.label }}</text>
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

    <view class="card card--coupon" @tap="toCoupons">
      <view class="coupon-entry">
        <text class="coupon-entry__icon">🎟️</text>
        <view class="coupon-entry__body">
          <text class="coupon-entry__title">我的优惠券</text>
          <text class="coupon-entry__desc">{{ couponText }}</text>
        </view>
        <text class="coupon-entry__arrow">›</text>
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
    flex: 1;
    margin-left: 24rpx;
  }

  &__arrow {
    margin-left: 12rpx;
    color: rgba(255, 255, 255, 0.8);
    font-size: 40rpx;
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

.stats {
  display: flex;
  align-items: center;
  /* 上移压住头图渐变，做出「卡片浮起来」的层次 */
  margin-top: -28rpx;

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__value {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-main;
  }

  &__label {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $text-secondary;
  }
}

.coupon-entry {
  display: flex;
  align-items: center;
  &__icon {
    font-size: 40rpx;
  }

  &__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    margin-left: 16rpx;
  }

  &__title {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-main;
  }

  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $text-secondary;
  }

  &__arrow {
    font-size: 34rpx;
    color: $text-placeholder;
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
