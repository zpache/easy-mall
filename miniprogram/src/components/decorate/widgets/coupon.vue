<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { couponApi } from '@/api/coupon'
import { decorateApi } from '@/api/decorate'
import { useUserStore } from '@/store/user'
import type { CouponCard } from '@/types/coupon'

/** 后端券卡 + 前端「已领张数」标记 */
type CardItem = CouponCard & { receivedCount?: number }

const props = defineProps<{ config: Record<string, any> }>()

const userStore = useUserStore()

const couponIds = computed<number[]>(() =>
  Array.isArray(props.config.couponIds)
    ? props.config.couponIds
        .map((id: any) => Number(id))
        .filter((id: number) => Number.isFinite(id) && id > 0)
    : []
)

/** 改造前的装修数据是手填券，未关联营销中心，仅做兜底展示 */
const legacyCoupons = computed<any[]>(() =>
  Array.isArray(props.config.coupons) ? props.config.coupons : []
)

const cards = ref<CardItem[]>([])
const claiming = ref(0)

const showLegacy = computed(() => couponIds.value.length === 0 && legacyCoupons.value.length > 0)
const visible = computed(() => cards.value.length > 0 || showLegacy.value)

async function loadCards() {
  if (couponIds.value.length === 0) return
  try {
    const list = await decorateApi.coupons(couponIds.value)
    const counts = await fetchReceivedCounts()
    cards.value = list.map((card) => ({ ...card, receivedCount: counts[card.id] || 0 }))
  } catch {
    // 券接口异常时整块不渲染，避免露出无法领取的空卡
  }
}

/** 已登录时同步已领张数：未使用 + 已使用的记录都算已领（已过期不占名额） */
async function fetchReceivedCounts(): Promise<Record<number, number>> {
  if (!userStore.isLogin) return {}
  try {
    const result = await couponApi.mine()
    const counts: Record<number, number> = {}
    for (const item of result.list) {
      if (item.status === 2) continue
      counts[item.couponId] = (counts[item.couponId] || 0) + 1
    }
    return counts
  } catch {
    return {}
  }
}

/** 已达每人限领张数 */
function isMaxed(card: CardItem) {
  return (card.receivedCount || 0) >= (card.perLimit || 1)
}

async function onClaim(card: CardItem) {
  if (claiming.value === card.id || isMaxed(card)) return
  if (card.soldOut) {
    uni.showToast({ title: '优惠券已被领完', icon: 'none' })
    return
  }
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  claiming.value = card.id
  try {
    await couponApi.receive(card.id)
    card.receivedCount = (card.receivedCount || 0) + 1
    if (card.remainCount > 0) {
      card.remainCount -= 1
      card.soldOut = card.remainCount === 0
    }
    uni.showToast({ title: '领取成功', icon: 'success' })
  } catch {
    // 业务提示（已领完 / 已达限领 / 已下架）由请求层统一 toast
  } finally {
    claiming.value = 0
  }
}

/** 历史手填券：未关联真实券，不能发券 */
function onLegacyClaim() {
  uni.showToast({ title: '该券未关联营销中心，请在装修后台重新选择', icon: 'none' })
}

function btnText(card: CardItem) {
  if (isMaxed(card)) return '已领取'
  if (card.soldOut) return '已领完'
  return '领取'
}

function legacyValue(item: Record<string, any>) {
  return item.discount ? `${(item.discount / 10).toFixed(1)}折` : `¥${item.amount}`
}

function legacyRule(item: Record<string, any>) {
  return Number(item.threshold) > 0 ? `满${item.threshold}可用` : '无门槛'
}

function toCouponList() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/coupon/index' })
}

onMounted(loadCards)
</script>

<template>
  <view v-if="visible" class="d-coupon" :style="{ background: config.bgColor || '#ffffff' }">
    <view class="d-coupon__head">
      <text class="d-coupon__title">{{ config.title || '领券中心' }}</text>
      <text class="d-coupon__more" @tap="toCouponList">更多 ›</text>
    </view>

    <view class="d-coupon__list">
      <!-- 真实优惠券 -->
      <view v-for="item in cards" :key="item.id" class="d-coupon__card">
        <view class="d-coupon__left">
          <text class="d-coupon__amount">{{ item.valueText }}</text>
          <text class="d-coupon__cond">{{ item.ruleText }}</text>
        </view>
        <view class="d-coupon__mid">
          <text class="d-coupon__name">{{ item.name }}</text>
          <text class="d-coupon__valid">{{ item.validText }}</text>
          <text v-if="item.remainCount >= 0" class="d-coupon__stock">剩余 {{ item.remainCount }} 张</text>
        </view>
        <text
          class="d-coupon__btn"
          :class="{ 'd-coupon__btn--off': isMaxed(item) || item.soldOut }"
          @tap="onClaim(item)"
        >
          {{ btnText(item) }}
        </text>
      </view>

      <!-- 历史手填券（未关联营销中心） -->
      <view v-for="(item, index) in showLegacy ? legacyCoupons : []" :key="index" class="d-coupon__card">
        <view class="d-coupon__left">
          <text class="d-coupon__amount">{{ legacyValue(item) }}</text>
          <text class="d-coupon__cond">{{ legacyRule(item) }}</text>
        </view>
        <view class="d-coupon__mid">
          <text class="d-coupon__name">{{ item.name }}</text>
          <text class="d-coupon__valid">{{ item.validDays }} 天内有效</text>
        </view>
        <text class="d-coupon__btn" @tap="onLegacyClaim">领取</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-coupon {
  padding: 24rpx;
}

.d-coupon__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.d-coupon__title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.d-coupon__more {
  font-size: 22rpx;
  color: #999;
}

.d-coupon__list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.d-coupon__card {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 16rpx 20rpx;
  background: linear-gradient(90deg, #fff5f5 0%, #ffffff 60%);
  border: 1rpx solid #ffe0e0;
  border-radius: 16rpx;
}

.d-coupon__left {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 124rpx;
  padding-right: 16rpx;
  border-right: 1rpx dashed #ffd0d0;
}

.d-coupon__amount {
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.1;
  color: #e02020;
}

.d-coupon__cond {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #e02020;
  text-align: center;
}

.d-coupon__mid {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.d-coupon__name {
  overflow: hidden;
  font-size: 24rpx;
  color: #333;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.d-coupon__valid {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #999;
}

.d-coupon__stock {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #bbb;
}

.d-coupon__btn {
  flex: none;
  padding: 8rpx 20rpx;
  font-size: 22rpx;
  color: #fff;
  background-color: #e02020;
  border-radius: 24rpx;

  &--off {
    color: #fff;
    background-color: #c8c9cc;
  }
}
</style>
