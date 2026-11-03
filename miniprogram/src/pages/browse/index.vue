<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { browseApi } from '@/api/browse'
import { formatPrice } from '@/utils/format'
import { resolveMediaUrl } from '@/utils/url'
import type { BrowseItem } from '@/types/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const items = ref<BrowseItem[]>([])
const loading = ref(true)
const pageNum = ref(1)
const finished = ref(false)
const total = ref(0)

/** 最近 7 天内的足迹直接显示「今天/昨天/x天前」，更早的显示日期 */
function timeText(value: string) {
  const time = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(time.getTime())) return value
  const days = Math.floor((Date.now() - time.getTime()) / 86_400_000)
  if (days <= 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  return value.slice(0, 10)
}

const totalText = computed(() => `共 ${total.value} 条足迹`)

async function load(reset = false) {
  if (reset) {
    pageNum.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const res = await browseApi.list(pageNum.value, 10)
    total.value = res.total
    items.value = reset ? res.records : [...items.value, ...res.records]
    finished.value = res.pages <= pageNum.value || res.records.length < 10
  } catch {
    // 错误提示由请求层统一 toast
  } finally {
    loading.value = false
  }
}

function goGoods(item: BrowseItem) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${item.goodsId}` })
}

onLoad(() => {
  if (!userStore.isLogin) {
    uni.redirectTo({ url: '/pages/login/index' })
    return
  }
  load(true)
})

onReachBottom(() => {
  if (!loading.value && !finished.value) {
    pageNum.value += 1
    load()
  }
})
</script>

<template>
  <view :class="{ 'page--empty': items.length === 0 }" class="page">
    <EmptyState v-if="!loading && items.length === 0" text="暂无浏览足迹" />
    <view v-else class="list">
      <view class="head">
        <text class="head__total">{{ totalText }}</text>
      </view>
      <view v-for="(item, index) in items" :key="`${item.goodsId}-${index}`" class="block">
        <!-- 第一条或与上一条日期不同时显示时间分组 -->
        <view
          v-if="index === 0 || timeText(item.browsedAt) !== timeText(items[index - 1].browsedAt)"
          class="block__time"
        >
          {{ timeText(item.browsedAt) }}
        </view>
        <view class="card" @tap="goGoods(item)">
          <image class="card__image" :src="resolveMediaUrl(item.mainImage)" mode="aspectFill" />
          <view class="card__body">
            <text class="card__name ellipsis-2">{{ item.goodsName }}</text>
            <view class="card__bottom">
              <text class="price">¥{{ formatPrice(item.price) }}</text>
              <text v-if="item.goodsStatus !== 1" class="card__offline">已下架</text>
              <text v-else class="card__time">{{ item.browsedAt.slice(11, 16) }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-if="finished && items.length > 0" class="list__end">
        最多保留最近 200 条足迹
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 40rpx;
  background-color: $page-bg;
  box-sizing: border-box;

  &--empty {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.head {
  padding: 8rpx 4rpx 16rpx;

  &__total {
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.block {
  &__time {
    padding: 12rpx 4rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: $text-secondary;
  }
}

.list {
  &__end {
    padding: 24rpx 0;
    font-size: 24rpx;
    color: $text-placeholder;
    text-align: center;
  }
}

.card {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  background-color: #fff;
  border-radius: 20rpx;

  &__image {
    flex-shrink: 0;
    width: 160rpx;
    height: 160rpx;
    border-radius: 12rpx;
    background-color: $page-bg;
  }

  &__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: space-between;
    min-width: 0;
  }

  &__name {
    font-size: 28rpx;
    color: $text-main;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__time {
    font-size: 22rpx;
    color: $text-placeholder;
  }

  &__offline {
    padding: 4rpx 16rpx;
    font-size: 22rpx;
    color: #c8c9cc;
    background-color: $page-bg;
    border-radius: 8rpx;
  }
}
</style>
