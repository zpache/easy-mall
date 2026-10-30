<script setup lang="ts">
import { ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { orderApi } from '@/api/order'
import { formatCountdown, formatPrice, formatTime } from '@/utils/format'
import { payOrder } from '@/utils/pay'
import type { Order } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

/** 用户端页签：status=10/11 为服务端状态组（待收货=已付款未收货、待评价=已完成且有未评商品） */
const tabs = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待付款', value: 0 },
  { label: '待收货', value: 10 },
  { label: '待评价', value: 11 },
  { label: '已完成', value: 3 }
]

const activeTab = ref(0)
const orders = ref<Order[]>([])
const loading = ref(false)
const pageNum = ref(1)
const finished = ref(false)

/* ---------------- 支付倒计时 ---------------- */

/** 每秒加 1，只作为响应式触发器，真实剩余时间按「服务端剩余秒数 - 本地流逝秒数」算 */
const tick = ref(0)
let tickTimer: ReturnType<typeof setInterval> | null = null
/** 本页数据最近一次刷新完成的时间戳 */
let syncedAt = 0
/** 有订单倒计时归零时刷新一次的最新时间，避免每秒钟连续刷新 */
let lastExpireRefresh = 0

function startTicking() {
  if (tickTimer) return
  tickTimer = setInterval(() => {
    tick.value += 1
    const expired = orders.value.some(
      (order) => order.status === 0 && remainSeconds(order) === 0
    )
    if (expired && Date.now() - lastExpireRefresh > 5000) {
      lastExpireRefresh = Date.now()
      load(true)
    }
  }, 1000)
}

function stopTicking() {
  if (!tickTimer) return
  clearInterval(tickTimer)
  tickTimer = null
}

/** 剩余秒数：-1 表示不展示（非待付款或服务端没给截止时间） */
function remainSeconds(order: Order): number {
  if (order.status !== 0 || order.payRemainSeconds === null || order.payRemainSeconds === undefined) {
    return -1
  }
  // 依赖滴答，保证每秒重算
  void tick.value
  if (!syncedAt) return -1
  return Math.max(Number(order.payRemainSeconds) - Math.floor((Date.now() - syncedAt) / 1000), 0)
}

function countdownText(order: Order): string {
  const remain = remainSeconds(order)
  return remain < 0 ? '' : formatCountdown(remain)
}

/** 用户端状态文案：商家待发货在用户视角统一呈现为「待收货」 */
function displayStatus(order: Order): string {
  return order.status === 1 ? '待收货' : order.statusText
}

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
    // 以服务端返回剩余秒数为基准，记录当前时刻用于本地递减
    syncedAt = Date.now()
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
  try {
    await payOrder(order.id)
  } catch (e) {
    const message = (e as Error)?.message
    uni.showToast({
      title: message === 'NOT_IN_WECHAT' ? '请在微信小程序内完成支付' : '支付已取消',
      icon: 'none'
    })
    return
  }
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

/** 确认收货：待收货 → 已完成 */
async function confirmReceive(order: Order) {
  const res = await uni.showModal({
    title: '确认收货',
    content: '请确认已收到全部商品，确认后订单将标记为已完成'
  })
  if (!res.confirm) return
  await orderApi.confirmReceive(order.id)
  uni.showToast({ title: '已确认收货', icon: 'success' })
  load(true)
}

/** 卡片整体可点：进订单详情；卡片内的操作按钮用 @tap.stop 拦住冒泡 */
function goDetail(order: Order) {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

/** 去评价：把订单内未评价的商品逐条填写 */
function goComment(order: Order) {
  uni.navigateTo({ url: `/pages/order/comment?id=${order.id}` })
}

/** 详情页里支付/取消后回列表要看到最新状态 */
function onOrderRefresh() {
  load(true)
}

onLoad((options) => {
  uni.$on('order:refresh', onOrderRefresh)
  const status = options?.status
  if (status !== undefined) {
    const index = tabs.findIndex((tab) => String(tab.value) === String(status))
    if (index >= 0) {
      activeTab.value = index
    }
  }
  load(true)
})

/** 首屏由 onLoad 拉取；从详情页/后台切回来时重新对齐倒计时基准 */
let firstShow = true
onShow(() => {
  startTicking()
  if (firstShow) {
    firstShow = false
    return
  }
  load(true)
})

onHide(stopTicking)

onUnload(() => {
  stopTicking()
  uni.$off('order:refresh', onOrderRefresh)
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

    <EmptyState v-if="!loading && !orders.length" text="暂无相关订单" />

    <view
      v-for="order in orders"
      :key="order.id"
      class="order"
      hover-class="order--hover"
      :hover-stay-time="80"
      @tap="goDetail(order)"
    >
      <view class="order__head">
        <text class="order__no">{{ order.orderNo }}</text>
        <view class="order__status-wrap">
          <text class="order__status">{{ displayStatus(order) }}</text>
          <text class="order__arrow">›</text>
        </view>
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
        <view class="order__meta">
          <text class="order__time">{{ formatTime(order.createdAt) }}</text>
          <text v-if="countdownText(order)" class="order__countdown">
            剩 {{ countdownText(order) }} 自动关闭
          </text>
        </view>
        <view class="order__amount">
          <text class="text-24rpx">实付</text>
          <text class="price text-30rpx"> ¥{{ formatPrice(order.payAmount) }}</text>
        </view>
      </view>

      <view v-if="order.status === 0" class="order__actions">
        <view class="btn btn--plain" @tap.stop="cancel(order)">取消订单</view>
        <view class="btn btn--primary" @tap.stop="pay(order)">去支付</view>
      </view>

      <!-- 待收货（含商家待发货）：可看物流信息；已发货的可确认收货 -->
      <view v-else-if="order.status === 1 || order.status === 2" class="order__actions">
        <view class="btn btn--plain" @tap.stop="goDetail(order)">物流信息</view>
        <view v-if="order.status === 2" class="btn btn--primary" @tap.stop="confirmReceive(order)">确认收货</view>
      </view>

      <!-- 已完成：还有未评价商品时给评价入口，全部评完显示已评价 -->
      <view v-else-if="order.status === 3" class="order__actions">
        <view
          v-if="(order.pendingCommentCount ?? 0) > 0"
          class="btn btn--primary"
          @tap.stop="goComment(order)"
        >
          去评价<text v-if="(order.pendingCommentCount ?? 0) > 1">（{{ order.pendingCommentCount }}）</text>
        </view>
        <text v-else class="order__commented">已评价</text>
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
  transition: background-color 0.15s;

  /* 整卡可点：按下时给一点底噪，提示可进入详情 */
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

  &__status-wrap {
    display: flex;
    align-items: center;
  }

  &__arrow {
    margin-left: 6rpx;
    font-size: 30rpx;
    line-height: 30rpx;
    color: $text-placeholder;
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

  &__meta {
    display: flex;
    flex-direction: column;
  }

  &__countdown {
    margin-top: 6rpx;
    font-size: 22rpx;
    font-weight: 600;
    color: $danger;
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

  &__commented {
    padding: 0 8rpx;
    height: 64rpx;
    line-height: 64rpx;
    font-size: 24rpx;
    color: $text-placeholder;
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
