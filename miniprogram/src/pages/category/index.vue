<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import GoodsCard from '@/components/goods-card.vue'
import EmptyState from '@/components/empty-state.vue'
import { goodsApi } from '@/api/goods'
import type { Category, Goods } from '@/types/api'

const categories = ref<Category[]>([])
const activeId = ref<number>(0)
const goodsList = ref<Goods[]>([])
const loading = ref(false)

async function loadCategories() {
  categories.value = await goodsApi.categories()
  const preferred = categories.value.find((item) => item.id === activeId.value) ?? categories.value[0]
  if (preferred) {
    await selectCategory(preferred.id)
  }
}

async function selectCategory(id: number) {
  activeId.value = id
  loading.value = true
  try {
    const page = await goodsApi.list({ categoryId: id, pageSize: 50 })
    goodsList.value = page.records
  } finally {
    loading.value = false
  }
}

// 首页点击分类时通过事件传递选中的分类
function onSelectFromHome(categoryId: number) {
  if (categories.value.some((item) => item.id === categoryId)) {
    selectCategory(categoryId)
  } else {
    activeId.value = categoryId
  }
}

onLoad(async () => {
  uni.$on('category:select', onSelectFromHome)
  await loadCategories()
})

onUnload(() => {
  uni.$off('category:select', onSelectFromHome)
})
</script>

<template>
  <view class="category-page">
    <scroll-view class="sidebar" scroll-y>
      <view
        v-for="category in categories"
        :key="category.id"
        class="sidebar__item"
        :class="{ 'sidebar__item--active': category.id === activeId }"
        @tap="selectCategory(category.id)"
      >
        {{ category.name }}
      </view>
    </scroll-view>

    <scroll-view class="content" scroll-y>
      <view v-if="loading" class="loading">加载中…</view>
      <EmptyState v-else-if="!goodsList.length" text="该分类暂无商品" />
      <template v-else>
        <GoodsCard
          v-for="item in goodsList"
          :key="item.id"
          class="goods-row"
          :goods="item"
          horizontal
        />
      </template>
    </scroll-view>
  </view>
</template>

<style scoped lang="scss">

.category-page {
  display: flex;
  height: 100vh;
  background-color: #fff;
}

.sidebar {
  width: 200rpx;
  height: 100%;
  background-color: $page-bg;

  &__item {
    padding: 32rpx 16rpx;
    font-size: 26rpx;
    color: $text-secondary;
    text-align: center;
    position: relative;

    &--active {
      background-color: #fff;
      color: $primary;
      font-weight: 600;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 32rpx;
        bottom: 32rpx;
        width: 6rpx;
        background-color: $primary;
        border-radius: 0 6rpx 6rpx 0;
      }
    }
  }
}

.content {
  flex: 1;
  height: 100%;
  padding: 12rpx 24rpx;
}

.goods-row + .goods-row {
  border-top: 1rpx solid $border-color;
}

.loading {
  padding: 80rpx 0;
  text-align: center;
  color: $text-placeholder;
  font-size: 26rpx;
}
</style>
