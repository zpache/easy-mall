<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onReachBottom, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { orderApi } from '@/api/order'
import { formatPrice, formatTime } from '@/utils/format'
import type { RefundRecord } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

/** 售后单状态：0 待审核 / 1 已同意待退款 / 2 已拒绝 / 3 已退款 / 4 已撤销 */
const tabs = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待审核', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已退款', value: 3 },
  { label: '已拒绝', value: 2 }
]

const activeTab = ref(0)
const records = ref<RefundRecord[]>([])
const loading = ref(false)
const pageNum = ref(1)
const finished = ref(false)

async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    pageNum.value = 1
    finished.value = false
    records.value = []
  }
  if (finished.value) return

  loading.value = true
  try {
    const page = await orderApi.refundList(pageNum.value, 10, tabs[activeTab.value].value)
    records.value = pageNum.value === 1 ? page.records : [...records.value, ...page.records]
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

/** 状态色：拒绝/撤销灰红区分，到账绿色，进行中主题色 */
function statusClass(record: RefundRecord): string {
  if (record.status === 2) return 'refund__status--danger'
  if (record.status === 3) return 'refund__status--success'
  if (record.status === 4) return 'refund__status--muted'
  return 'refund__status--active'
}

function goDetail(record: RefundRecord) {
  uni.navigateTo({ url: `/pages/order/refund-detail?id=${record.id}` })
}

function goOrder(record: RefundRecord) {
  uni.navigateTo({ url: `/pages/order/detail?id=${record.orderId}` })
}

onLoad(() => {
  load(true)
})

/** 从详情页撤销/返回时刷新最新状态 */
let firstShow = true
onShow(() => {
  if (firstShow) {
    firstShow = false
    return
  }
  load(true)
})

onReachBottom(() => {
  load()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
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

    <EmptyState v-if="!loading && !records.length" text="暂无售后记录" />

    <view
      v-for="record in records"
      :key="record.id"
      class="refund"
      hover-class="refund--hover"
      :hover-stay-time="80"
      @tap="goDetail(record)"
    >
      <view class="refund__head">
        <text class="refund__no">{{ record.refundNo }}</text>
        <text class="refund__status" :class="statusClass(record)">{{ record.statusText }}</text>
      </view>

      <view class="refund__body">
        <view class="refund__row">
          <text class="refund__label">售后类型</text>
          <text class="refund__value">{{ record.typeText }}</text>
        </view>
        <view class="refund__row">
          <text class="refund__label">申请原因</text>
          <text class="refund__value ellipsis-1">{{ record.reason }}</text>
        </view>
        <view class="refund__row">
          <text class="refund__label">退款金额</text>
          <text class="refund__amount">¥{{ formatPrice(record.amount) }}</text>
        </view>
        <view class="refund__row">
          <text class="refund__label">申请时间</text>
          <text class="refund__value">{{ formatTime(record.appliedAt) }}</text>
        </view>
      </view>

      <view class="refund__foot">
        <view class="refund__order" @tap.stop="goOrder(record)">
          <text>订单 {{ record.orderNo }}</text>
          <text class="refund__arrow">›</text>
        </view>
        <view class="refund__progress">
          {{ record.status === 0 ? '等待商家审核' : '查看进度' }}
          <text class="refund__arrow">›</text>
        </view>
      </view>
    </view>

    <view v-if="records.length" class="load-more" @tap="load()">
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

.refund {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;
  transition: background-color 0.15s;

  &--hover {
    background-color: #fafafa;
  }

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
    font-weight: 600;

    &--active {
      color: $primary;
    }

    &--success {
      color: #52c41a;
    }

    &--danger {
      color: $danger;
    }

    &--muted {
      color: $text-placeholder;
    }
  }

  &__body {
    padding: 8rpx 0;
  }

  &__row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 8rpx 0;
  }

  &__label {
    flex-shrink: 0;
    margin-right: 24rpx;
    font-size: 24rpx;
    color: $text-placeholder;
  }

  &__value {
    flex: 1;
    text-align: right;
    font-size: 24rpx;
    color: $text-main;
  }

  &__amount {
    font-size: 28rpx;
    font-weight: 600;
    color: $danger;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 16rpx;
    border-top: 1rpx solid $border-color;
  }

  &__order {
    display: flex;
    align-items: center;
    font-size: 22rpx;
    color: $text-secondary;
  }

  &__progress {
    display: flex;
    align-items: center;
    font-size: 24rpx;
    color: $primary;
  }

  &__arrow {
    margin-left: 4rpx;
    font-size: 28rpx;
    line-height: 24rpx;
    color: $text-placeholder;
  }
}

.load-more {
  padding: 32rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
