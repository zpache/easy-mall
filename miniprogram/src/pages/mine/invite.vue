<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow, onShareAppMessage } from '@dcloudio/uni-app'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import { inviteApi } from '@/api/user'
import type { InviteRecord } from '@/api/user'
import { useUserStore } from '@/store/user'
import { useDecorateStore } from '@/store/decorate'
import { resolveMediaUrl } from '@/utils/url'
import { appMessage, enableShareMenu, SHOP_NAME, sharePath } from '@/utils/share'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const userStore = useUserStore()
const decorateStore = useDecorateStore()

const invitedCount = ref(0)
const rewardPoints = ref(0)
const records = ref<InviteRecord[]>([])
const loaded = ref(false)

const nickname = computed(() => userStore.profile?.nickname || '')

async function load() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  try {
    const res = await inviteApi.records(1, 50)
    invitedCount.value = res.invitedCount
    rewardPoints.value = res.rewardPoints
    records.value = res.records.records
  } catch {
    // 请求层已提示，页面保持空态
  } finally {
    loaded.value = true
  }
}

function statusText(item: InviteRecord) {
  return item.status === 1 ? '已下单' : '已注册'
}

function formatTime(value?: string) {
  return value ? String(value).slice(0, 10) : ''
}

onShow(() => {
  enableShareMenu()
  load()
})

/** 转发标题：昵称邀请更有说服力；落地首页并带邀请人 */
const shareTitle = computed(() => {
  const shop = decorateStore.nav.navbar.title || SHOP_NAME
  return nickname.value ? `${nickname.value} 邀请你逛${shop}` : `发现一个好物商城：${shop}`
})

onShareAppMessage(() => appMessage(shareTitle.value, sharePath('/pages/index/index')))
</script>

<template>
  <!-- 主题变量注入 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page">
    <view class="hero">
      <view class="hero__stat">
        <text class="hero__value">{{ invitedCount }}</text>
        <text class="hero__label">邀请好友</text>
      </view>
      <view class="hero__stat">
        <text class="hero__value">{{ rewardPoints }}</text>
        <text class="hero__label">累计奖励积分</text>
      </view>
    </view>

    <view class="card rules">
      <text class="rules__title">玩法说明</text>
      <text class="rules__item">1. 点下方按钮把商城分享给微信好友；</text>
      <text class="rules__item">2. 好友通过你的分享进入并注册，你得注册积分奖励；</text>
      <text class="rules__item">3. 好友完成首单支付，你再得额外积分奖励；</text>
      <text class="rules__item">4. 积分在「我的 - 可用积分」查看，具体数额以活动配置为准。</text>
    </view>

    <view class="card">
      <view class="card__head">
        <text class="card__title">邀请记录</text>
      </view>
      <view v-if="!records.length && loaded" class="empty">
        <text>还没有邀请记录，快去分享吧</text>
      </view>
      <view v-for="item in records" :key="item.id" class="record">
        <image
          v-if="item.avatar"
          class="record__avatar"
          :src="resolveMediaUrl(item.avatar)"
          mode="aspectFill"
        />
        <view v-else class="record__avatar record__avatar--placeholder">
          {{ (item.nickname || '客').slice(0, 1) }}
        </view>
        <view class="record__info">
          <text class="record__name">{{ item.nickname || '微信用户' }}</text>
          <text class="record__time">{{ formatTime(item.createdAt) }}</text>
        </view>
        <view class="record__right">
          <text class="record__points">
            +{{ (item.registerPoints || 0) + (item.orderPoints || 0) }} 积分
          </text>
          <text class="record__status" :class="{ 'record__status--done': item.status === 1 }">
            {{ statusText(item) }}
          </text>
        </view>
      </view>
    </view>

    <!-- 分享按钮：open-type=share 触发本页 onShareAppMessage -->
    <button class="invite-btn" open-type="share" hover-class="invite-btn--hover">
      邀请好友，一起逛
    </button>

    <DecorateTabbar :config="decorateStore.nav.tabbar" current="/pages/mine/index" />
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 180rpx;
}

.hero {
  display: flex;
  padding: 56rpx 0;
  background: linear-gradient(135deg, #ff8a4c 0%, $primary 100%);

  &__stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__value {
    color: #fff;
    font-size: 48rpx;
    font-weight: 600;
  }

  &__label {
    margin-top: 10rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
  }
}

.card {
  margin: 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }
}

.rules {
  &__title {
    display: block;
    margin-bottom: 14rpx;
    font-size: 28rpx;
    font-weight: 600;
  }

  &__item {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    line-height: 36rpx;
    color: $text-secondary;
  }
}

.empty {
  padding: 48rpx 0;
  font-size: 26rpx;
  color: $text-placeholder;
  text-align: center;
}

.record {
  display: flex;
  align-items: center;
  padding: 20rpx 0;

  & + & {
    border-top: 1rpx solid $border-color;
  }

  &__avatar {
    flex: none;
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background-color: #f2f2f2;

    &--placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      color: $text-secondary;
      font-size: 30rpx;
    }
  }

  &__info {
    flex: 1;
    margin-left: 20rpx;
  }

  &__name {
    display: block;
    font-size: 28rpx;
    color: $text-main;
  }

  &__time {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $text-placeholder;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }

  &__points {
    font-size: 26rpx;
    font-weight: 600;
    color: $price;
  }

  &__status {
    margin-top: 6rpx;
    padding: 2rpx 12rpx;
    font-size: 20rpx;
    color: #b7791f;
    background-color: #fdf6e3;
    border-radius: 8rpx;

    &--done {
      color: #2f8a4d;
      background-color: #ecf7ef;
    }
  }
}

/* 分享按钮：button 默认样式重置 */
.invite-btn {
  margin: 32rpx 24rpx;
  padding: 26rpx 0;
  color: #fff;
  font-size: 30rpx;
  line-height: normal;
  text-align: center;
  background-color: $primary;
  border: none;
  border-radius: 44rpx;
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.12);

  &::after {
    border: none;
  }

  &--hover {
    opacity: 0.85;
  }
}
</style>
