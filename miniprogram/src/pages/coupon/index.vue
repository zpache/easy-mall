<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { couponApi } from '@/api/coupon'
import type { MemberCoupon } from '@/types/coupon'

const tabs = [
  { label: '全部', value: undefined as number | undefined },
  { label: '可用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已过期', value: 2 }
]

const activeTab = ref(0)
const list = ref<MemberCoupon[]>([])
const counts = ref({ available: 0, used: 0, expired: 0 })
const loading = ref(false)
const loaded = ref(false)

const tabList = computed(() =>
  tabs.map((tab, index) => ({
    ...tab,
    badge:
      index === 1 ? counts.value.available : index === 2 ? counts.value.used : index === 3 ? counts.value.expired : 0
  }))
)

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await couponApi.mine(tabs[activeTab.value].value)
    list.value = res.list
    counts.value = { available: res.available, used: res.used, expired: res.expired }
    loaded.value = true
  } catch {
    // request 层已提示，页面保留上一次数据
  } finally {
    loading.value = false
  }
}

function switchTab(index: number) {
  if (index === activeTab.value) return
  activeTab.value = index
  load()
}

function statusText(coupon: MemberCoupon): string {
  if (coupon.status === 0) return '可用'
  if (coupon.status === 1) return '已使用'
  return '已过期'
}

function timeText(coupon: MemberCoupon): string {
  const day = (value?: string | null) => (value ? value.slice(0, 10) : '')
  if (coupon.status === 0) {
    return coupon.expireAt ? `有效期至 ${day(coupon.expireAt)}` : '长期有效'
  }
  if (coupon.status === 1) {
    return coupon.usedAt ? `使用于 ${day(coupon.usedAt)}` : '已使用'
  }
  return '已过期'
}

const emptyText = computed(() => {
  if (activeTab.value === 0) return '还没有优惠券'
  return `暂无${tabs[activeTab.value].label}的优惠券`
})

onShow(() => {
  load()
})
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="(tab, index) in tabList"
        :key="tab.label"
        class="tabs__item"
        :class="{ 'tabs__item--active': index === activeTab }"
        @tap="switchTab(index)"
      >
        <text>{{ tab.label }}</text>
        <text v-if="tab.badge > 0" class="tabs__badge">{{ tab.badge }}</text>
      </view>
    </view>

    <EmptyState v-if="loaded && !loading && !list.length" :text="emptyText" tip="参与店铺活动即可获得优惠券" />

    <view v-for="coupon in list" :key="coupon.id" class="coupon" :class="{ 'coupon--off': coupon.status !== 0 }">
      <view class="coupon__left">
        <text class="coupon__value">{{ coupon.valueText }}</text>
        <text class="coupon__rule">{{ coupon.ruleText }}</text>
      </view>

      <view class="coupon__right">
        <view class="coupon__head">
          <text class="coupon__name">{{ coupon.name }}</text>
          <text class="coupon__status">{{ statusText(coupon) }}</text>
        </view>
        <text class="coupon__time">{{ timeText(coupon) }}</text>
        <text v-if="coupon.orderNo" class="coupon__order">订单号 {{ coupon.orderNo }}</text>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中…</view>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 40rpx;
}

.tabs {
  display: flex;
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #fff;

  &__item {
    position: relative;
    flex: 1;
    padding: 24rpx 0;
    font-size: 26rpx;
    color: $text-secondary;
    text-align: center;

    &--active {
      color: $primary;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 8rpx;
        left: 50%;
        width: 48rpx;
        height: 4rpx;
        background-color: $primary;
        border-radius: 4rpx;
        transform: translateX(-50%);
      }
    }
  }

  &__badge {
    margin-left: 4rpx;
    font-size: 20rpx;
    color: $primary;
  }
}

.coupon {
  display: flex;
  overflow: hidden;
  margin: 20rpx 24rpx;
  background-color: #fff;
  border-radius: 16rpx;

  &--off {
    opacity: 0.55;
  }

  &__left {
    display: flex;
    flex: none;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 200rpx;
    padding: 28rpx 12rpx;
    background: linear-gradient(135deg, #ff8a4c 0%, $primary 100%);
  }

  &__value {
    color: #fff;
    font-size: 42rpx;
    font-weight: 700;
  }

  &__rule {
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.9);
    font-size: 20rpx;
  }

  &__right {
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
    padding: 24rpx;
    border-left: 2rpx dashed $border-color;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12rpx;
  }

  &__name {
    overflow: hidden;
    flex: 1;
    font-size: 28rpx;
    font-weight: 600;
    color: $text-main;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__status {
    flex: none;
    font-size: 22rpx;
    color: $primary;
  }

  &__time {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: $text-secondary;
  }

  &__order {
    margin-top: 4rpx;
    font-size: 22rpx;
    color: $text-placeholder;
  }
}

.coupon--off .coupon__left {
  background: linear-gradient(135deg, #c8c8c8 0%, #a8a8a8 100%);
}

.coupon--off .coupon__status {
  color: $text-secondary;
}

.loading {
  padding: 40rpx 0;
  color: $text-placeholder;
  font-size: 24rpx;
  text-align: center;
}
</style>
