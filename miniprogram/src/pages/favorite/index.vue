<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { favoriteApi } from '@/api/favorite'
import { formatPrice } from '@/utils/format'
import { resolveMediaUrl } from '@/utils/url'
import type { FavoriteItem } from '@/types/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const items = ref<FavoriteItem[]>([])
const loading = ref(true)
const pageNum = ref(1)
const finished = ref(false)

const totalText = computed(() => `共 ${total.value} 件收藏`)
const total = ref(0)

async function load(reset = false) {
  if (reset) {
    pageNum.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const res = await favoriteApi.list(pageNum.value, 10)
    total.value = res.total
    items.value = reset ? res.records : [...items.value, ...res.records]
    finished.value = res.pages <= pageNum.value || res.records.length < 10
  } catch {
    // 错误提示由请求层统一 toast，这里保证页面落到空态
  } finally {
    loading.value = false
  }
}

/** 取消收藏：本地移除并刷新总数 */
async function removeFavorite(item: FavoriteItem) {
  try {
    await favoriteApi.toggle(item.goodsId)
    items.value = items.value.filter((it) => it.goodsId !== item.goodsId)
    total.value = Math.max(total.value - 1, 0)
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch {
    // 错误提示由请求层统一 toast
  }
}

function goGoods(item: FavoriteItem) {
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
    <EmptyState v-if="!loading && items.length === 0" text="还没有收藏，去逛逛吧" />
    <view v-else class="list">
      <view class="head">
        <text class="head__total">{{ totalText }}</text>
      </view>
      <view v-for="item in items" :key="item.goodsId" class="card" @tap="goGoods(item)">
        <image class="card__image" :src="resolveMediaUrl(item.mainImage)" mode="aspectFill" />
        <view class="card__body">
          <text class="card__name ellipsis-2">{{ item.goodsName }}</text>
          <view class="card__bottom">
            <view>
              <text class="price">¥{{ formatPrice(item.price) }}</text>
              <text v-if="Number(item.originalPrice) > 0" class="card__origin">
                ¥{{ formatPrice(item.originalPrice) }}
              </text>
            </view>
            <!-- 下架/已删除商品仍保留展示，但不能购买 -->
            <text
              v-if="item.goodsStatus !== 1"
              class="card__offline"
              @tap.stop="removeFavorite(item)"
            >
              已失效，点击移除
            </text>
            <text v-else class="card__remove" @tap.stop="removeFavorite(item)">取消收藏</text>
          </view>
        </view>
      </view>
      <view v-if="finished && items.length > 0" class="list__end">没有更多收藏了</view>
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
    width: 180rpx;
    height: 180rpx;
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

  &__origin {
    margin-left: 8rpx;
    font-size: 22rpx;
    color: $text-placeholder;
    text-decoration: line-through;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__remove {
    padding: 6rpx 20rpx;
    font-size: 22rpx;
    color: $text-secondary;
    border: 1rpx solid $border-color;
    border-radius: 24rpx;
  }

  &__offline {
    padding: 6rpx 20rpx;
    font-size: 22rpx;
    color: #c8c9cc;
    border: 1rpx solid $border-color;
    border-radius: 24rpx;
  }
}
</style>
