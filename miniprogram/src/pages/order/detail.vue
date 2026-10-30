<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { orderApi } from '@/api/order'
import { formatCountdown, formatPrice, formatTime } from '@/utils/format'
import { payOrder } from '@/utils/pay'
import type { Order } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const orderId = ref(0)
const order = ref<Order | null>(null)
const loading = ref(true)

/* ---------------- 支付倒计时 ---------------- */

/** 每秒加 1，仅用于驱动重算；真实剩余时间来自服务端 payRemainSeconds 减本地流逝 */
const tick = ref(0)
let tickTimer: ReturnType<typeof setInterval> | null = null
/** 本页数据最近一次同步时间 */
let syncedAt = 0
/** 上次因倒计时归零而刷新的时间，避免连续刷新 */
let lastExpireRefresh = 0

const remainSeconds = computed(() => {
  void tick.value
  const current = order.value
  if (!current || current.status !== 0 || current.payRemainSeconds === null || current.payRemainSeconds === undefined) {
    return -1
  }
  if (!syncedAt) return -1
  return Math.max(Number(current.payRemainSeconds) - Math.floor((Date.now() - syncedAt) / 1000), 0)
})

const countdownText = computed(() =>
  remainSeconds.value < 0 ? '' : formatCountdown(remainSeconds.value)
)

function startTicking() {
  if (tickTimer) return
  tickTimer = setInterval(() => {
    tick.value += 1
    // 倒计时结束：订单已被服务端关单，重新拉一次详情把状态切成「已取消」
    if (remainSeconds.value === 0 && Date.now() - lastExpireRefresh > 5000) {
      lastExpireRefresh = Date.now()
      load()
    }
  }, 1000)
}

function stopTicking() {
  if (!tickTimer) return
  clearInterval(tickTimer)
  tickTimer = null
}

/** 状态说明：给状态位补一句人话，避免只有「待发货」三个字 */
const STATUS_TIPS: Record<number, string> = {
  0: '请尽快完成支付，超时订单将自动关闭',
  1: '商家正在备货，请耐心等待发货',
  2: '商品已发出，请注意查收',
  3: '交易已完成，感谢你的购买',
  4: '订单已取消，库存已释放'
}

const statusTip = computed(() => (order.value ? STATUS_TIPS[order.value.status] ?? '' : ''))

/** 已取消订单用中性灰，其余状态用主色 */
const isCancelled = computed(() => order.value?.status === 4)

const freightText = computed(() => {
  const amount = Number(order.value?.freightAmount ?? 0)
  return amount > 0 ? `¥${formatPrice(amount)}` : '包邮'
})

const couponAmount = computed(() => Number(order.value?.couponAmount ?? 0))
const memberDiscount = computed(() => Number(order.value?.memberDiscount ?? 0))

/** 商品总额：下单快照里没有单独的字段，用实付反推口径容易对不上，这里按下单明细求和 */
const goodsAmount = computed(() => {
  if (!order.value) return 0
  const sum = order.value.items.reduce((total, item) => total + Number(item.price) * item.quantity, 0)
  // 明细缺失时退回「应付 - 运费 + 各项优惠」的老口径，保证金额区不出现 0
  if (!sum) {
    return Number(order.value.payAmount) - Number(order.value.freightAmount ?? 0)
      + couponAmount.value + memberDiscount.value
  }
  return sum
})

async function load() {
  loading.value = true
  try {
    order.value = await orderApi.detail(orderId.value)
    // 以服务端剩余秒数为基准，记录此刻用于本地递减
    syncedAt = Date.now()
  } catch {
    // 错误提示由请求层统一 toast，这里只保证页面落到空态
    order.value = null
  } finally {
    loading.value = false
  }
}

async function pay() {
  try {
    await payOrder(orderId.value)
  } catch (e) {
    const message = (e as Error)?.message
    uni.showToast({
      title: message === 'NOT_IN_WECHAT' ? '请在微信小程序内完成支付' : '支付已取消',
      icon: 'none'
    })
    return
  }
  uni.showToast({ title: '支付成功', icon: 'success' })
  uni.$emit('order:refresh')
  load()
}

async function cancel() {
  const res = await uni.showModal({ title: '提示', content: '确定取消该订单吗？' })
  if (!res.confirm) return
  try {
    await orderApi.cancel(orderId.value)
  } catch {
    return
  }
  uni.showToast({ title: '订单已取消', icon: 'none' })
  uni.$emit('order:refresh')
  load()
}

function copyOrderNo() {
  if (!order.value) return
  uni.setClipboardData({ data: order.value.orderNo })
}

function copyDeliveryNo() {
  if (!order.value?.deliveryNo) return
  uni.setClipboardData({ data: order.value.deliveryNo })
}

/** 确认收货：待收货 → 已完成，服务端带状态条件更新，重复点击不会出错 */
async function confirmReceive() {
  const res = await uni.showModal({
    title: '确认收货',
    content: '请确认已收到全部商品，确认后订单将标记为已完成'
  })
  if (!res.confirm) return
  try {
    await orderApi.confirmReceive(orderId.value)
  } catch {
    return
  }
  uni.showToast({ title: '已确认收货', icon: 'success' })
  uni.$emit('order:refresh')
  load()
}

/** 能否申请退款：已支付、未取消，且没有处理中/已完成的售后 */
const canApplyRefund = computed(() => {
  const current = order.value
  if (!current) return false
  if (current.status !== 1 && current.status !== 2) return false
  return !current.refundStatus || current.refundStatus === 3
})

async function applyRefund() {
  const res = await uni.showModal({
    title: '申请退款',
    editable: true,
    placeholderText: '请填写退款原因（选填）',
    content: ''
  })
  if (!res.confirm) return
  try {
    await orderApi.applyRefund({
      orderId: orderId.value,
      reason: (res as any).content || '',
      type: 'refund'
    })
  } catch {
    return
  }
  uni.showToast({ title: '已提交，等待商家处理', icon: 'none' })
  uni.$emit('order:refresh')
  load()
}

function goGoods(goodsId: number) {
  if (!goodsId) return
  uni.navigateTo({ url: `/pages/goods/detail?id=${goodsId}` })
}

/** 待评价商品数：已完成订单里还没有评价的商品 */
const pendingComments = computed(() => Number(order.value?.pendingCommentCount ?? 0))

/** 去评价：填写本单未评价的商品 */
function goComment() {
  uni.navigateTo({ url: `/pages/order/comment?id=${orderId.value}` })
}

onLoad((options) => {
  orderId.value = Number(options?.id ?? 0)
  if (!orderId.value) {
    loading.value = false
    return
  }
  load()
  startTicking()
})

/** 首屏由 onLoad 拉取；从评价页等子页面返回时刷新，保证已评价状态及时更新 */
let firstShow = true
onShow(() => {
  if (firstShow) {
    firstShow = false
    return
  }
  if (orderId.value) load()
})

onUnload(stopTicking)
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page" :class="{ 'page--with-bar': order?.status === 0 || order?.status === 2 || canApplyRefund || pendingComments > 0 }">
    <view v-if="loading" class="loading">加载中…</view>

    <EmptyState v-else-if="!order" text="订单不存在" tip="可能已被删除，可返回订单列表刷新" />

    <template v-if="order">
      <view class="status" :class="{ 'status--grey': isCancelled }">
        <text class="status__text">{{ order.statusText }}</text>
        <text v-if="countdownText" class="status__countdown">
          剩余 {{ countdownText }} 自动关闭
        </text>
        <text v-if="order.refundStatusText" class="status__refund">{{ order.refundStatusText }}</text>
        <text v-if="statusTip" class="status__tip">{{ statusTip }}</text>
      </view>

      <view v-if="order.receiverName" class="card receiver">
        <view class="receiver__head">
          <text class="receiver__name">{{ order.receiverName }}</text>
          <text class="receiver__phone">{{ order.receiverPhone }}</text>
        </view>
        <text class="receiver__detail">{{ order.receiverAddress }}</text>
      </view>

      <!-- 物流信息：后台发货后才有单号，已发货订单与已完成订单都会展示 -->
      <view v-if="order.deliveryNo" class="card logistics">
        <view class="logistics__head">
          <text class="logistics__title">{{ order.deliveryCompany || '快递发货' }}</text>
          <text class="logistics__copy" @tap.stop="copyDeliveryNo">复制单号</text>
        </view>
        <text class="logistics__no">{{ order.deliveryNo }}</text>
        <text v-if="order.deliveryTime" class="logistics__time">
          {{ formatTime(order.deliveryTime) }} 已发货
        </text>
      </view>

      <view class="card">
        <view v-for="item in order.items" :key="item.id" class="goods" @tap="goGoods(item.goodsId)">
          <image class="goods__image" :src="item.goodsImage" mode="aspectFill" />
          <view class="goods__body">
            <text class="goods__name ellipsis-2">{{ item.goodsName }}</text>
            <view class="goods__bottom">
              <text class="price">¥{{ formatPrice(item.price) }}</text>
              <view class="goods__right">
                <!-- 已完成订单：单个商品也可直接点评价，已评价的打标 -->
                <text v-if="item.commented" class="goods__commented">已评价</text>
                <text
                  v-else-if="order.status === 3"
                  class="goods__tocomment"
                  @tap.stop="goComment"
                >
                  评价
                </text>
                <text class="goods__qty">x{{ item.quantity }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="card rows">
        <view class="row">
          <text class="row__label">商品总额</text>
          <text class="row__value">¥{{ formatPrice(goodsAmount) }}</text>
        </view>
        <view class="row">
          <text class="row__label">运费</text>
          <text class="row__value">{{ freightText }}</text>
        </view>
        <view v-if="memberDiscount > 0" class="row">
          <text class="row__label">会员折扣{{ order.memberLevelName ? `（${order.memberLevelName}）` : '' }}</text>
          <text class="row__value row__value--cut">-¥{{ formatPrice(memberDiscount) }}</text>
        </view>
        <view v-if="couponAmount > 0" class="row">
          <text class="row__label">优惠券</text>
          <text class="row__value row__value--cut">-¥{{ formatPrice(couponAmount) }}</text>
        </view>
        <view class="row row--total">
          <text class="row__label">实付款</text>
          <text class="price text-34rpx">¥{{ formatPrice(order.payAmount) }}</text>
        </view>
      </view>

      <view class="card rows">
        <view class="row">
          <text class="row__label">订单编号</text>
          <view class="row__right">
            <text class="row__value">{{ order.orderNo }}</text>
            <text class="row__copy" @tap.stop="copyOrderNo">复制</text>
          </view>
        </view>
        <view class="row">
          <text class="row__label">下单时间</text>
          <text class="row__value">{{ formatTime(order.createdAt) }}</text>
        </view>
        <view v-if="order.remark" class="row">
          <text class="row__label">订单备注</text>
          <text class="row__value row__value--wrap">{{ order.remark }}</text>
        </view>
      </view>

      <view v-if="order.status === 0" class="bar">
        <view class="bar__btn bar__btn--plain" hover-class="bar__btn--hover" :hover-stay-time="80" @tap="cancel">
          取消订单
        </view>
        <view class="bar__btn bar__btn--primary" hover-class="bar__btn--hover" :hover-stay-time="80" @tap="pay">
          去支付
        </view>
      </view>

      <view v-else-if="canApplyRefund || order.status === 2 || pendingComments > 0" class="bar">
        <view v-if="canApplyRefund" class="bar__btn bar__btn--plain" hover-class="bar__btn--hover" :hover-stay-time="80" @tap="applyRefund">
          申请退款
        </view>
        <view v-if="order.status === 2" class="bar__btn bar__btn--primary" hover-class="bar__btn--hover" :hover-stay-time="80" @tap="confirmReceive">
          确认收货
        </view>
        <view v-if="pendingComments > 0" class="bar__btn bar__btn--primary" hover-class="bar__btn--hover" :hover-stay-time="80" @tap="goComment">
          评价商品<text v-if="pendingComments > 1">（{{ pendingComments }}）</text>
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding: 20rpx 24rpx 40rpx;

  &--with-bar {
    padding-bottom: 180rpx;
  }
}

.loading {
  padding: 160rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: $text-placeholder;
}

.status {  padding: 36rpx 28rpx;
  border-radius: 20rpx;
  background-color: $primary-light;

  &--grey {
    background-color: #f0f0f0;

    .status__text {
      color: $text-main;
    }

    .status__tip {
      color: $text-secondary;
    }
  }

  &__text {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
    color: $primary;
  }

  &__tip {
    display: block;
    margin-top: 10rpx;
    font-size: 24rpx;
    color: $text-secondary;
  }

  &__countdown {
    display: inline-block;
    margin-top: 12rpx;
    padding: 4rpx 14rpx;
    font-size: 24rpx;
    font-weight: 600;
    color: #fff;
    background-color: $danger;
    border-radius: 999rpx;
  }

  &__refund {
    display: inline-block;
    margin-top: 12rpx;
    margin-left: 12rpx;
    padding: 4rpx 14rpx;
    font-size: 24rpx;
    font-weight: 600;
    color: #fff;
    background-color: #f59e0b;
    border-radius: 999rpx;
  }
}

.card {
  margin-top: 20rpx;
  padding: 0 24rpx;
  background-color: #fff;
  border-radius: 20rpx;
}

.logistics {
  padding: 24rpx;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 28rpx;
    font-weight: 600;
  }

  &__copy {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    line-height: 32rpx;
    color: $primary;
    border: 1rpx solid $primary;
    border-radius: 24rpx;
  }

  &__no {
    display: block;
    margin-top: 12rpx;
    font-size: 30rpx;
    font-weight: 600;
    letter-spacing: 1rpx;
    word-break: break-all;
  }

  &__time {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.receiver {
  padding: 28rpx 24rpx;

  &__head {
    display: flex;
    align-items: baseline;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__phone {
    margin-left: 20rpx;
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__detail {
    display: block;
    margin-top: 12rpx;
    font-size: 26rpx;
    line-height: 38rpx;
    color: $text-secondary;
  }
}

.goods {
  display: flex;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $border-color;

  &:last-child {
    border-bottom: none;
  }

  &__image {
    width: 140rpx;
    height: 140rpx;
    border-radius: 12rpx;
    background-color: #f2f2f2;
  }

  &__body {
    flex: 1;
    min-width: 0;
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

  &__right {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__commented {
    padding: 2rpx 12rpx;
    font-size: 22rpx;
    color: $text-placeholder;
    border: 1rpx solid $border-color;
    border-radius: 20rpx;
  }

  &__tocomment {
    padding: 2rpx 16rpx;
    font-size: 22rpx;
    color: $primary;
    border: 1rpx solid $primary;
    border-radius: 20rpx;
  }

  &__qty {
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.rows {
  padding: 8rpx 24rpx;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0;
  font-size: 26rpx;
  border-bottom: 1rpx solid $border-color;

  &:last-child {
    border-bottom: none;
  }

  &--total {
    .row__label {
      color: $text-main;
      font-weight: 600;
    }
  }

  &__label {
    flex: none;
    color: $text-secondary;
  }

  &__right {
    display: flex;
    align-items: center;
  }

  &__value {
    max-width: 440rpx;
    text-align: right;

    &--cut {
      color: $price;
    }

    &--wrap {
      flex: 1;
      margin-left: 32rpx;
      word-break: break-all;
    }
  }

  &__copy {
    margin-left: 16rpx;
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    line-height: 32rpx;
    color: $primary;
    border: 1rpx solid $primary;
    border-radius: 24rpx;
  }
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);

  &__btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 84rpx;
    border-radius: 42rpx;
    font-size: 30rpx;
    font-weight: 600;
    letter-spacing: 1rpx;
    transition: opacity 0.15s, transform 0.15s;
  }

  &__btn--plain {
    margin-right: 20rpx;
    color: $primary;
    background-color: $primary-light;
    box-shadow: inset 0 0 0 1rpx $primary;
  }

  &__btn--primary {
    color: #fff;
    background-color: $primary;
    box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.12);
  }

  /* 只有一个按钮时靠右、不铺满整行 */
  &--single {
    justify-content: flex-end;

    .bar__btn {
      flex: 0 0 260rpx;
    }
  }

  &__btn--hover {
    opacity: 0.82;
    transform: scale(0.985);
  }
}
</style>
