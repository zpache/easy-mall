<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps<{ config: Record<string, any> }>()

/** 剩余时间：超过 24 小时自动进位到「天」 */
const remain = ref({ days: 0, hours: 0, minutes: 0, seconds: 0 })
let timer: any

const pad2 = (n: number) => String(n).padStart(2, '0')

function tick() {
  const end = props.config.endTime ? new Date(String(props.config.endTime).replace(/-/g, '/')).getTime() : 0
  const diff = Math.max(0, end - Date.now())
  remain.value = {
    days: Math.floor(diff / 86_400_000),
    hours: Math.floor((diff % 86_400_000) / 3_600_000),
    minutes: Math.floor((diff % 3_600_000) / 60_000),
    seconds: Math.floor((diff % 60_000) / 1000)
  }
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})

onBeforeUnmount(() => clearInterval(timer))

const price = computed(() => Number(props.config.price ?? 0).toFixed(2))
const originPrice = computed(() => Number(props.config.originPrice ?? 0).toFixed(2))

/**
 * 倒计时片段：
 * - 满 24 小时：20天 : 1小时 : 03分 : 04秒
 * - 不足 24 小时：1541:03:25 的纯数字形式（保持原有观感）
 */
const segments = computed(() => {
  const { days, hours, minutes, seconds } = remain.value
  if (days > 0) {
    return [
      { num: String(days), unit: '天' },
      { num: String(hours), unit: '小时' },
      { num: pad2(minutes), unit: '分' },
      { num: pad2(seconds), unit: '秒' }
    ]
  }
  return [
    { num: pad2(hours), unit: '' },
    { num: pad2(minutes), unit: '' },
    { num: pad2(seconds), unit: '' }
  ]
})

</script>

<template>
  <view class="d-seckill" :style="{ background: config.bgColor || '#fff1f0' }">
    <view class="d-seckill__head">
      <view class="d-seckill__head-left">
        <text class="d-seckill__flag">秒杀</text>
        <text class="d-seckill__title">{{ config.title || '限时秒杀' }}</text>
        <text v-if="config.subtitle" class="d-seckill__sub">{{ config.subtitle }}</text>
      </view>
      <view class="d-seckill__countdown">
        <template v-for="(seg, i) in segments" :key="i">
          <text v-if="i > 0" class="d-seckill__colon">:</text>
          <text class="d-seckill__num">{{ seg.num }}</text>
          <text v-if="seg.unit" class="d-seckill__unit">{{ seg.unit }}</text>
        </template>
      </view>
    </view>

    <view class="d-seckill__goods">
      <image v-if="config.image" class="d-seckill__cover" :src="config.image" mode="aspectFill" />
      <view v-else class="d-seckill__cover d-seckill__cover--empty" />
      <view class="d-seckill__info">
        <text class="d-seckill__name">{{ config.goodsName || '秒杀商品' }}</text>
        <view class="d-seckill__prices">
          <text class="d-seckill__price"><text class="d-seckill__yuan">¥</text>{{ price }}</text>
          <text class="d-seckill__origin">¥{{ originPrice }}</text>
        </view>
        <text class="d-seckill__btn">立即抢购</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.d-seckill {
  padding: 24rpx;
}

.d-seckill__head {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.d-seckill__head-left {
  display: flex;
  gap: 12rpx;
  align-items: center;
  min-width: 0;
}

.d-seckill__flag {
  padding: 2rpx 12rpx;
  font-size: 20rpx;
  color: #fff;
  background-color: #e02020;
  border-radius: 6rpx;
}

.d-seckill__title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.d-seckill__sub {
  overflow: hidden;
  font-size: 20rpx;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.d-seckill__countdown {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  gap: 4rpx;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
}

.d-seckill__num {
  padding: 4rpx 8rpx;
  font-size: 20rpx;
  color: #fff;
  background-color: #333;
  border-radius: 6rpx;
}

.d-seckill__colon {
  font-size: 20rpx;
  color: #333;
}

.d-seckill__unit {
  font-size: 20rpx;
  color: #333;
}

.d-seckill__goods {
  display: flex;
  gap: 20rpx;
  padding: 16rpx;
  background-color: #fff;
  border-radius: 16rpx;
}

.d-seckill__cover {
  flex: none;
  width: 192rpx;
  height: 192rpx;
  border-radius: 12rpx;
}

.d-seckill__cover--empty {
  background-color: #eef0f2;
}

.d-seckill__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.d-seckill__name {
  display: -webkit-box;
  overflow: hidden;
  font-size: 26rpx;
  color: #333;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.d-seckill__prices {
  display: flex;
  gap: 12rpx;
  align-items: baseline;
  margin-top: auto;
}

.d-seckill__price {
  font-size: 36rpx;
  font-weight: 700;
  color: #e02020;
}

.d-seckill__yuan {
  font-size: 22rpx;
}

.d-seckill__origin {
  font-size: 22rpx;
  color: #999;
  text-decoration: line-through;
}

.d-seckill__btn {
  align-self: flex-start;
  padding: 8rpx 24rpx;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #fff;
  background: linear-gradient(90deg, #ff6b00, #e02020);
  border-radius: 24rpx;
}
</style>
