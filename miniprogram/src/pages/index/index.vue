<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import GoodsCard from '@/components/goods-card.vue'
import EmptyState from '@/components/empty-state.vue'
import { goodsApi } from '@/api/goods'
import type { Goods, HomeData } from '@/types/api'

const home = ref<HomeData>({ categories: [], hotGoods: [], newGoods: [] })
const loading = ref(true)
const keyword = ref('')
const searchResult = ref<Goods[]>([])
const searching = ref(false)

async function loadHome() {
  try {
    home.value = await goodsApi.home()
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  const text = keyword.value.trim()
  if (!text) {
    searchResult.value = []
    searching.value = false
    return
  }
  const page = await goodsApi.list({ keyword: text, pageSize: 20 })
  searchResult.value = page.records
  searching.value = true
}

function clearSearch() {
  keyword.value = ''
  searchResult.value = []
  searching.value = false
}

function toCategory(categoryId?: number) {
  uni.switchTab({
    url: '/pages/category/index',
    success: () => {
      if (categoryId) {
        uni.$emit('category:select', categoryId)
      }
    }
  })
}

onLoad(() => {
  loadHome()
})

onPullDownRefresh(async () => {
  await loadHome()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="hero__title">easy-mall</text>
      <text class="hero__desc">精选好物 · 产地直发</text>
    </view>

    <view class="search">
      <input
        v-model="keyword"
        class="search__input"
        type="text"
        confirm-type="search"
        placeholder="搜索商品"
        placeholder-class="search__placeholder"
        @confirm="onSearch"
      />
      <text v-if="searching" class="search__cancel" @tap="clearSearch">取消</text>
      <text v-else class="search__btn" @tap="onSearch">搜索</text>
    </view>

    <template v-if="searching">
      <view class="section">
        <view class="section__head">
          <text class="section__title">搜索结果</text>
        </view>
        <EmptyState v-if="!searchResult.length" text="没有找到相关商品" />
        <GoodsCard
          v-for="item in searchResult"
          :key="item.id"
          class="goods-row"
          :goods="item"
          horizontal
        />
      </view>
    </template>

    <template v-else>
      <view class="section">
        <view class="section__head">
          <text class="section__title">全部分类</text>
          <text class="section__more" @tap="toCategory()">查看全部</text>
        </view>
        <view class="category-grid">
          <view
            v-for="category in home.categories"
            :key="category.id"
            class="category-grid__item"
            @tap="toCategory(category.id)"
          >
            <view class="category-grid__icon">{{ category.name.slice(0, 1) }}</view>
            <text class="category-grid__name">{{ category.name }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section__head">
          <text class="section__title">热销好物</text>
        </view>
        <EmptyState v-if="!loading && !home.hotGoods.length" text="暂无商品" />
        <GoodsCard
          v-for="item in home.hotGoods"
          :key="item.id"
          class="goods-row"
          :goods="item"
          horizontal
        />
      </view>

      <view class="section">
        <view class="section__head">
          <text class="section__title">新品上架</text>
        </view>
        <view class="goods-grid">
          <GoodsCard v-for="item in home.newGoods" :key="item.id" :goods="item" />
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 40rpx;
}

.hero {
  padding: 48rpx 32rpx 64rpx;
  background: linear-gradient(135deg, #ff8a4c 0%, $primary 100%);
  color: #fff;

  &__title {
    display: block;
    font-size: 44rpx;
    font-weight: 700;
  }

  &__desc {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    opacity: 0.85;
  }
}

.search {
  display: flex;
  align-items: center;
  margin: -36rpx 24rpx 0;
  padding: 0 24rpx;
  height: 88rpx;
  background-color: #fff;
  border-radius: 44rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);

  &__input {
    flex: 1;
    font-size: 28rpx;
  }

  &__placeholder {
    color: $text-placeholder;
  }

  &__btn,
  &__cancel {
    margin-left: 16rpx;
    font-size: 26rpx;
    color: $primary;
  }
}

.section {
  margin: 32rpx 24rpx 0;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-main;
  }

  &__more {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

.category-grid {
  display: flex;
  flex-wrap: wrap;

  &__item {
    width: 20%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12rpx 0;
  }

  &__icon {
    width: 84rpx;
    height: 84rpx;
    border-radius: 50%;
    background-color: $primary-light;
    color: $primary;
    font-size: 32rpx;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__name {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: $text-secondary;
  }
}

.goods-row + .goods-row {
  border-top: 1rpx solid $border-color;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}
</style>
