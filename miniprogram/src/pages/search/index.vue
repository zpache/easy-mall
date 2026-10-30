<script setup lang="ts">
import { ref } from 'vue'
import GoodsCard from '@/components/goods-card.vue'
import EmptyState from '@/components/empty-state.vue'
import { goodsApi } from '@/api/goods'
import type { Goods } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const keyword = ref('')
const list = ref<Goods[]>([])
const searching = ref(false)
const searched = ref(false)

async function onSearch() {
  const text = keyword.value.trim()
  if (!text) {
    list.value = []
    searched.value = false
    return
  }
  searching.value = true
  try {
    const page = await goodsApi.list({ keyword: text, pageSize: 30 })
    list.value = page.records
    searched.value = true
  } finally {
    searching.value = false
  }
}

function goBack() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
}
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="search-page">
    <view class="search-page__bar">
      <input
        v-model="keyword"
        class="search-page__input"
        type="text"
        confirm-type="search"
        focus
        placeholder="搜索商品"
        placeholder-class="search-page__ph"
        @confirm="onSearch"
      />
      <text class="search-page__btn" @tap="onSearch">搜索</text>
      <text class="search-page__btn search-page__btn--ghost" @tap="goBack">返回</text>
    </view>

    <view class="search-page__body">
      <view v-if="searching" class="search-page__state">搜索中…</view>
      <EmptyState v-else-if="searched && !list.length" text="没有找到相关商品" />
      <EmptyState v-else-if="!searched" text="输入关键词，找到想要的好物" />
      <view v-else>
        <GoodsCard
          v-for="item in list"
          :key="item.id"
          class="search-page__row"
          :goods="item"
          horizontal
        />
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.search-page {
  min-height: 100vh;
  background-color: $page-bg;
}

.search-page__bar {
  display: flex;
  gap: 16rpx;
  align-items: center;
  padding: 20rpx 24rpx;
  background-color: #fff;
}

.search-page__input {
  flex: 1;
  height: 68rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
  background-color: $page-bg;
  border-radius: 34rpx;
}

.search-page__ph {
  color: $text-placeholder;
}

.search-page__btn {
  flex: none;
  font-size: 26rpx;
  color: $primary;
}

.search-page__btn--ghost {
  color: $text-secondary;
}

.search-page__body {
  padding: 24rpx;
}

.search-page__row {
  padding: 20rpx;
  margin-bottom: 16rpx;
  background-color: #fff;
  border-radius: 16rpx;
}

.search-page__state {
  padding: 80rpx 0;
  font-size: 26rpx;
  color: $text-secondary;
  text-align: center;
}
</style>
