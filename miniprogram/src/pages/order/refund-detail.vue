<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { orderApi } from '@/api/order'
import { formatPrice, formatTime } from '@/utils/format'
import type { RefundRecord } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const refundId = ref(0)
const record = ref<null | RefundRecord>(null)
const loading = ref(true)

/**
 * 进度时间线：三步「提交申请 → 商家审核 → 退款到账」。
 * 拒绝 / 撤销在第二步终止，用不同状态标记。
 */
const steps = computed(() => {
  const current = record.value
  if (!current) return []
  const status = current.status
  return [
    {
      title: '提交申请',
      desc: current.appliedAt ? formatTime(current.appliedAt) : '',
      state: 'done'
    },
    {
      title: '商家审核',
      desc: status === 0
        ? '等待商家处理'
        : (current.handledAt ? formatTime(current.handledAt) : ''),
      // 2 已拒绝 / 4 已撤销：终态；1 已同意、3 已退款：通过
      state: status === 0 ? 'active' : status === 2 ? 'failed' : status === 4 ? 'cancelled' : 'done'
    },
    {
      title: status === 2 ? '流程终止' : status === 4 ? '已撤销' : '退款到账',
      desc: status === 3 && current.wxRefundNo ? `微信退款单号 ${current.wxRefundNo}` : '',
      state: status === 3 ? 'done' : status === 2 || status === 4 ? 'cancelled' : 'pending'
    }
  ]
})

const rejectReason = computed(() => {
  const current = record.value
  if (!current) return ''
  return current.status === 2 || current.status === 4 ? (current.auditRemark || '') : ''
})

async function load() {
  if (!refundId.value) return
  loading.value = true
  try {
    record.value = await orderApi.refundDetail(refundId.value)
  } finally {
    loading.value = false
  }
}

/** 撤销申请：仅待审核可撤，撤后可重新申请 */
async function cancelApply() {
  const res = await uni.showModal({
    title: '撤销售后',
    content: '确定撤销本次退款申请吗？撤销后可重新发起申请'
  })
  if (!res.confirm) return
  try {
    await orderApi.cancelRefund(refundId.value)
  } catch {
    return
  }
  uni.showToast({ title: '已撤销', icon: 'none' })
  load()
}

function goOrder() {
  const current = record.value
  if (!current) return
  uni.navigateTo({ url: `/pages/order/detail?id=${current.orderId}` })
}

onLoad((options) => {
  refundId.value = Number(options?.id ?? 0)
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page">
    <template v-if="record">
      <!-- 状态头 -->
      <view class="hero">
        <text class="hero__status">{{ record.statusText }}</text>
        <text class="hero__amount">退款金额 ¥{{ formatPrice(record.amount) }}</text>
        <text v-if="record.status === 0" class="hero__tip">商家会尽快处理，结果将通过订阅消息通知</text>
        <text v-else-if="record.status === 3" class="hero__tip">退款已原路退回你的支付账户，注意查收</text>
      </view>

      <!-- 进度时间线 -->
      <view class="card timeline">
        <view class="timeline__title">退款进度</view>
        <view v-for="(step, index) in steps" :key="step.title" class="step">
          <view class="step__rail">
            <view class="step__dot" :class="`step__dot--${step.state}`" />
            <view
              v-if="index < steps.length - 1"
              class="step__line"
              :class="{ 'step__line--done': step.state === 'done' }"
            />
          </view>
          <view class="step__body">
            <text
              class="step__name"
              :class="{
                'step__name--done': step.state === 'done',
                'step__name--active': step.state === 'active',
                'step__name--failed': step.state === 'failed',
                'step__name--muted': step.state === 'cancelled' || step.state === 'pending'
              }"
            >
              {{ step.title }}
            </text>
            <text v-if="step.desc" class="step__desc">{{ step.desc }}</text>
          </view>
        </view>

        <view v-if="rejectReason" class="timeline__reject">
          <text>{{ record?.status === 2 ? '商家拒绝原因' : '撤销信息' }}：{{ rejectReason }}</text>
        </view>
      </view>

      <!-- 售后信息 -->
      <view class="card info">
        <view class="info__title">售后信息</view>
        <view class="info__row">
          <text class="info__label">售后单号</text>
          <text class="info__value">{{ record.refundNo }}</text>
        </view>
        <view class="info__row" @tap="goOrder">
          <text class="info__label">关联订单</text>
          <view class="info__order">
            <text class="info__value">{{ record.orderNo }}</text>
            <text class="info__arrow">›</text>
          </view>
        </view>
        <view class="info__row">
          <text class="info__label">售后类型</text>
          <text class="info__value">{{ record.typeText }}</text>
        </view>
        <view class="info__row">
          <text class="info__label">订单实付</text>
          <text class="info__value">¥{{ formatPrice(record.orderPayAmount) }}</text>
        </view>
        <view class="info__row">
          <text class="info__label">申请原因</text>
          <text class="info__value">{{ record.reason }}</text>
        </view>
      </view>

      <!-- 待审核时允许撤销 -->
      <view v-if="record.status === 0" class="actions">
        <view class="actions__btn actions__btn--plain" hover-class="actions__btn--hover" :hover-stay-time="80" @tap="cancelApply">
          撤销申请
        </view>
      </view>
    </template>

    <view v-if="loading && !record" class="page__loading">加载中…</view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 40rpx;

  &__loading {
    padding: 120rpx 0;
    text-align: center;
    font-size: 26rpx;
    color: $text-placeholder;
  }
}

.hero {
  padding: 48rpx 32rpx;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__status {
    font-size: 38rpx;
    font-weight: 600;
    color: $text-main;
  }

  &__amount {
    margin-top: 16rpx;
    font-size: 30rpx;
    font-weight: 600;
    color: $danger;
  }

  &__tip {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: $text-secondary;
  }
}

.card {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;
}

.timeline {
  &__title,
  .info__title {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-main;
    margin-bottom: 24rpx;
  }

  &__reject {
    margin-top: 16rpx;
    padding: 16rpx 20rpx;
    background-color: #fff7e6;
    border-radius: 12rpx;
    font-size: 24rpx;
    line-height: 34rpx;
    color: #ad6800;
  }
}

.step {
  display: flex;

  &__rail {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 40rpx;
    margin-right: 20rpx;
  }

  &__dot {
    width: 20rpx;
    height: 20rpx;
    border-radius: 50%;
    background-color: $border-color;

    &--done {
      background-color: $primary;
    }

    &--active {
      background-color: #fff;
      border: 6rpx solid $primary;
      width: 20rpx;
      height: 20rpx;
    }

    &--failed {
      background-color: $danger;
    }

    &--cancelled {
      background-color: $text-placeholder;
    }
  }

  &__line {
    flex: 1;
    width: 2rpx;
    min-height: 56rpx;
    margin: 6rpx 0;
    background-color: $border-color;

    &--done {
      background-color: $primary;
    }
  }

  &__body {
    flex: 1;
    padding-bottom: 40rpx;
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: 28rpx;
    color: $text-placeholder;

    &--done {
      color: $text-main;
    }

    &--active {
      color: $primary;
      font-weight: 600;
    }

    &--failed {
      color: $danger;
      font-weight: 600;
    }

    &--muted {
      color: $text-placeholder;
    }
  }

  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    line-height: 30rpx;
    color: $text-secondary;
    word-break: break-all;
  }
}

.info {
  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12rpx 0;
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
    word-break: break-all;
  }

  &__order {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }

  &__arrow {
    margin-left: 4rpx;
    font-size: 28rpx;
    line-height: 24rpx;
    color: $text-placeholder;
  }
}

.actions {
  margin: 40rpx 24rpx;
  display: flex;

  &__btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    border-radius: 40rpx;
    font-size: 28rpx;

    &--plain {
      border: 1rpx solid $border-color;
      color: $text-secondary;
      background-color: #fff;
    }

    &--hover {
      opacity: 0.8;
    }
  }
}
</style>
