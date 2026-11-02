<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage, onShareTimeline, onShow, onUnload } from '@dcloudio/uni-app'
import GoodsCard from '@/components/goods-card.vue'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import EmptyState from '@/components/empty-state.vue'
import { goodsApi } from '@/api/goods'
import { useDecorateStore } from '@/store/decorate'
import { appMessage, captureInviter, enableShareMenu, SHOP_NAME, sharePath, timelineMessage } from '@/utils/share'
import type { Category, Goods } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const decorateStore = useDecorateStore()

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

onLoad(async (options) => {
  // 分享卡片进入：记录邀请人 + 打开转发/朋友圈入口
  captureInviter(options)
  enableShareMenu()
  uni.$on('category:select', onSelectFromHome)
  decorateStore.ensureNav()
  await loadCategories()
})

onShow(() => {
  // 切回分类 tab 时按 TTL 校验线上主题
  decorateStore.ensureTheme()
})

onUnload(() => {
  uni.$off('category:select', onSelectFromHome)
})

/** 分享标题跟随当前选中的分类 */
const shareTitle = computed(() => {
  const category = categories.value.find((item) => item.id === activeId.value)
  return category ? `${category.name} · 好物推荐` : `${SHOP_NAME} · 全部分类`
})

/** 转发落地到商品列表页（带分类），比落地分类 tab 更直接 */
onShareAppMessage(() =>
  appMessage(
    shareTitle.value,
    sharePath('/pages/goods/list', { categoryId: activeId.value || undefined }),
    goodsList.value[0]?.mainImage
  )
)

onShareTimeline(() =>
  timelineMessage(
    shareTitle.value,
    { categoryId: activeId.value || undefined },
    goodsList.value[0]?.mainImage
  )
)
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
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

    <DecorateTabbar :config="decorateStore.nav.tabbar" current="/pages/category/index" />
  </view>
</template>

<style scoped lang="scss">

.category-page {
  display: flex;
  box-sizing: border-box;
  height: 100vh;
  padding-bottom: 104rpx;
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
