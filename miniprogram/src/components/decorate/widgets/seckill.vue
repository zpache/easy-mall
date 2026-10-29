<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { openLink } from '@/utils/decorate'

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

/**
 * 秒杀商品列表：新版为装修后台商品选择器的快照数组（含 seckillPrice 与真实商品 id）；
 * 旧版手填单商品字段做兼容展示。
 */
const goodsList = computed<any[]>(() => {
  if (Array.isArray(props.config.goods) && props.config.goods.length > 0) {
    return props.config.goods
  }
  const { goodsName, image, price, originPrice } = props.config
  if (goodsName || image) {
    return [
      {
        image: image || '',
        name: goodsName || '秒杀商品',
        price: Number(originPrice ?? 0),
        seckillPrice: Number(price ?? 0)
      }
    ]
  }
  return []
})

function priceOf(item: any) {
  return Number(item.seckillPrice ?? item.price ?? 0).toFixed(2)
}

function originOf(item: any) {
  const origin = Number(item.price ?? 0)
  const seckill = Number(item.seckillPrice ?? 0)
  return seckill > 0 && origin > seckill ? origin.toFixed(2) : ''
}

function toDetail(item: any) {
  // 新数据有真实商品 id；旧手填数据走历史 link 字段兜底
  if (item.id) {
    uni.navigateTo({ url: `/pages/goods/detail?id=${item.id}` })
    return
  }
  openLink(props.config.link)
}

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

    <scroll-view v-if="goodsList.length > 0" class="d-seckill__scroll" scroll-x>
      <view
        v-for="(item, index) in goodsList"
        :key="item.id ?? index"
        class="d-seckill__goods"
        @tap="toDetail(item)"
      >
        <image v-if="item.image" class="d-seckill__cover" :src="item.image" mode="aspectFill" />
        <view v-else class="d-seckill__cover d-seckill__cover--empty" />
        <view class="d-seckill__info">
          <text class="d-seckill__name">{{ item.name || '秒杀商品' }}</text>
          <view class="d-seckill__prices">
            <text class="d-seckill__price"><text class="d-seckill__yuan">¥</text>{{ priceOf(item) }}</text>
            <text v-if="originOf(item)" class="d-seckill__origin">¥{{ originOf(item) }}</text>
          </view>
          <text class="d-seckill__btn">立即抢购</text>
        </view>
      </view>
    </scroll-view>
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

.d-seckill__scroll {
  width: 100%;
  white-space: nowrap;
}

.d-seckill__goods {
  display: inline-flex;
  gap: 20rpx;
  width: 480rpx;
  padding: 16rpx;
  margin-right: 16rpx;
  overflow: hidden;
  background-color: #fff;
  border-radius: 16rpx;
  vertical-align: top;
  white-space: normal;
}

.d-seckill__cover {
  flex: none;
  width: 168rpx;
  height: 168rpx;
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
