<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { orderApi } from '@/api/order'
import { formatPrice, formatTime } from '@/utils/format'
import type { Order } from '@/types/api'

const tabs = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 }
]

const activeTab = ref(0)
const orders = ref<Order[]>([])
const loading = ref(false)
const pageNum = ref(1)
const finished = ref(false)

async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    finished.value = false
    orders.value = []
  }
  if (finished.value) return

  loading.value = true
  try {
    const page = await orderApi.list(tabs[activeTab.value].value, pageNum.value, 10)
    orders.value = pageNum.value === 1 ? page.records : [...orders.value, ...page.records]
    finished.value = pageNum.value >= page.pages
    pageNum.value += 1
  } finally {
    loading.value = false
  }
}

function switchTab(index: number) {
  if (activeTab.value === index) return
  activeTab.value = index
  load(true)
}

async function pay(order: Order) {
  await orderApi.pay(order.id)
  uni.showToast({ title: '支付成功', icon: 'success' })
  load(true)
}

async function cancel(order: Order) {
  const res = await uni.showModal({ title: '提示', content: '确定取消该订单吗？' })
  if (!res.confirm) return
  await orderApi.cancel(order.id)
  uni.showToast({ title: '订单已取消', icon: 'none' })
  load(true)
}

onLoad((options) => {
  const status = options?.status
  if (status !== undefined) {
    const index = tabs.findIndex((tab) => String(tab.value) === String(status))
    if (index >= 0) {
      activeTab.value = index
    }
  }
  load(true)
})
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="(tab, index) in tabs"
        :key="tab.label"
        class="tabs__item"
        :class="{ 'tabs__item--active': index === activeTab }"
        @tap="switchTab(index)"
      >
        {{ tab.label }}
      </view>
    </view>

    <EmptyState v-if="!loading && !orders.length" text="暂无相关订单" />

    <view v-for="order in orders" :key="order.id" class="order">
      <view class="order__head">
        <text class="order__no">{{ order.orderNo }}</text>
        <text class="order__status">{{ order.statusText }}</text>
      </view>

      <view v-for="item in order.items" :key="item.id" class="goods">
        <image class="goods__image" :src="item.goodsImage" mode="aspectFill" />
        <view class="goods__body">
          <text class="goods__name ellipsis-2">{{ item.goodsName }}</text>
          <view class="goods__bottom">
            <text class="price">¥{{ formatPrice(item.price) }}</text>
            <text class="goods__qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>

      <view class="order__foot">
        <text class="order__time">{{ formatTime(order.createdAt) }}</text>
        <view class="order__amount">
          <text class="text-24rpx">实付</text>
          <text class="price text-30rpx"> ¥{{ formatPrice(order.payAmount) }}</text>
        </view>
      </view>

      <view v-if="order.status === 0" class="order__actions">
        <view class="btn btn--plain" @tap="cancel(order)">取消订单</view>
        <view class="btn btn--primary" @tap="pay(order)">去支付</view>
      </view>
    </view>

    <view v-if="orders.length" class="load-more" @tap="load()">
      {{ finished ? '没有更多了' : loading ? '加载中…' : '加载更多' }}
    </view>
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
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 10;

  &__item {
    flex: 1;
    padding: 24rpx 0;
    text-align: center;
    font-size: 26rpx;
    color: $text-secondary;

    &--active {
      color: $primary;
      font-weight: 600;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 8rpx;
        width: 48rpx;
        height: 4rpx;
        background-color: $primary;
        border-radius: 4rpx;
        transform: translateX(-50%);
      }
    }
  }
}

.order {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 16rpx;
    border-bottom: 1rpx solid $border-color;
  }

  &__no {
    font-size: 24rpx;
    color: $text-secondary;
  }

  &__status {
    font-size: 26rpx;
    color: $primary;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 16rpx;
    border-top: 1rpx solid $border-color;
  }

  &__time {
    font-size: 22rpx;
    color: $text-placeholder;
  }

  &__amount {
    display: flex;
    align-items: baseline;
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 16rpx;
    margin-top: 20rpx;
  }
}

.goods {
  display: flex;
  padding: 20rpx 0;

  &__image {
    width: 130rpx;
    height: 130rpx;
    border-radius: 12rpx;
    background-color: #f2f2f2;
  }

  &__body {
    flex: 1;
    margin-left: 20rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    line-height: 38rpx;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__qty {
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.btn {
  padding: 0 32rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 32rpx;
  font-size: 26rpx;

  &--plain {
    border: 1rpx solid $border-color;
    color: $text-secondary;
  }

  &--primary {
    background-color: $primary;
    color: #fff;
  }
}

.load-more {
  padding: 32rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
