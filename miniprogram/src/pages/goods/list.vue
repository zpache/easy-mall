<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import GoodsCard from '@/components/goods-card.vue'
import { goodsApi } from '@/api/goods'
import type { Category, Goods } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

/** 每页固定 10 条，页面滚动到底部自动加载下一页 */
const PAGE_SIZE = 10

type SortValue = '' | 'sales' | 'priceAsc' | 'priceDesc'

const categories = ref<Category[]>([])
/** 0 表示「全部」，即默认展示全品类商品 */
const activeCategoryId = ref(0)

const keyword = ref('')
/** 实际生效的关键词：只有点搜索/回车才生效，避免输入过程中频繁请求 */
const appliedKeyword = ref('')
const sortBy = ref<SortValue>('')

const list = ref<Goods[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const finished = ref(false)

const sortTabs = [
  { label: '综合', value: '' },
  { label: '销量', value: 'sales' },
  { label: '价格', value: 'price' }
]

const footerText = computed(() => {
  if (loading.value) return '加载中…'
  if (finished.value) return list.value.length ? '没有更多了' : ''
  return '上拉加载更多'
})

async function loadCategories() {
  try {
    categories.value = await goodsApi.categories()
  } catch {
    // 分类拉取失败不阻断商品列表，仍可按全品类浏览
  }
}

/** 加载中又切换了筛选条件时，等本次请求结束后补一次重置加载，避免点击被丢弃 */
let pendingReset = false

async function load(reset = false) {
  if (loading.value) {
    if (reset) pendingReset = true
    return
  }
  if (reset) {
    // 条件变化时清空旧列表，避免切分类/搜索时先闪一屏旧数据
    pageNum.value = 1
    finished.value = false
    list.value = []
    total.value = 0
  }
  if (!reset && finished.value) return

  loading.value = true
  try {
    const page = await goodsApi.list({
      categoryId: activeCategoryId.value || undefined,
      keyword: appliedKeyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      sortBy: sortBy.value || undefined
    })
    const records = page.records ?? []
    list.value = pageNum.value === 1 ? records : list.value.concat(records)
    total.value = page.total ?? list.value.length
    finished.value = pageNum.value >= (page.pages || 1)
    pageNum.value += 1
  } catch {
    // 请求层已统一提示，这里只结束加载态
  } finally {
    loading.value = false
    if (pendingReset) {
      pendingReset = false
      load(true)
    }
  }
}

function selectCategory(id: number) {
  if (activeCategoryId.value === id) return
  activeCategoryId.value = id
  load(true)
}

function onSearch() {
  appliedKeyword.value = keyword.value.trim()
  load(true)
}

function clearKeyword() {
  keyword.value = ''
  if (appliedKeyword.value) {
    appliedKeyword.value = ''
    load(true)
  }
}

function isSortActive(value: string) {
  if (value === 'price') return sortBy.value === 'priceAsc' || sortBy.value === 'priceDesc'
  return sortBy.value === value
}

function onSortTap(value: string) {
  if (value === 'price') {
    // 价格在升序/降序之间切换
    sortBy.value = sortBy.value === 'priceAsc' ? 'priceDesc' : 'priceAsc'
  } else {
    sortBy.value = value as SortValue
  }
  load(true)
}

onLoad((options) => {
  // 支持从其它页面带初始条件进入：?categoryId=1&keyword=脐橙
  const categoryId = Number(options?.categoryId)
  if (Number.isFinite(categoryId) && categoryId > 0) {
    activeCategoryId.value = categoryId
  }
  const kw = String(options?.keyword || '').trim()
  if (kw) {
    keyword.value = kw
    appliedKeyword.value = kw
  }
  loadCategories()
  load(true)
})

onReachBottom(() => {
  load()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="goods-page">
    <view class="filter">
      <view class="filter__search">
        <text class="filter__icon">🔍</text>
        <input
          v-model="keyword"
          class="filter__input"
          type="text"
          confirm-type="search"
          placeholder="搜索商品"
          placeholder-class="filter__ph"
          @confirm="onSearch"
        />
        <text v-if="keyword" class="filter__clear" @tap="clearKeyword">✕</text>
        <text class="filter__btn" @tap="onSearch">搜索</text>
      </view>

      <scroll-view class="filter__cats" scroll-x :show-scrollbar="false">
        <view
          class="filter__cat"
          :class="{ 'filter__cat--active': activeCategoryId === 0 }"
          @tap="selectCategory(0)"
        >
          全部
        </view>
        <view
          v-for="category in categories"
          :key="category.id"
          class="filter__cat"
          :class="{ 'filter__cat--active': activeCategoryId === category.id }"
          @tap="selectCategory(category.id)"
        >
          {{ category.name }}
        </view>
      </scroll-view>

      <view class="filter__sorts">
        <view
          v-for="tab in sortTabs"
          :key="tab.value"
          class="filter__sort"
          :class="{ 'filter__sort--active': isSortActive(tab.value) }"
          @tap="onSortTap(tab.value)"
        >
          {{ tab.label }}
          <text v-if="tab.value === 'price' && isSortActive('price')" class="filter__arrow">
            {{ sortBy === 'priceAsc' ? '↑' : '↓' }}
          </text>
        </view>
        <text v-if="total" class="filter__total">共 {{ total }} 件</text>
      </view>
    </view>

    <view class="body">
      <view v-if="loading && !list.length" class="body__state">加载中…</view>
      <EmptyState
        v-else-if="!list.length"
        :text="appliedKeyword ? '没有找到相关商品' : '该分类下暂无商品'"
      />

      <view v-else class="grid">
        <view v-for="item in list" :key="item.id" class="grid__item">
          <GoodsCard :goods="item" />
        </view>
      </view>

      <view v-if="list.length" class="foot">{{ footerText }}</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.goods-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
  background-color: $page-bg;
}

.filter {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 16rpx 0 8rpx;
  background-color: #fff;
}

.filter__search {
  display: flex;
  gap: 16rpx;
  align-items: center;
  height: 68rpx;
  padding: 0 24rpx;
  margin: 0 24rpx;
  background-color: $page-bg;
  border-radius: 34rpx;
}

.filter__icon {
  flex: none;
  font-size: 24rpx;
}

.filter__input {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
}

.filter__ph {
  color: $text-placeholder;
}

.filter__clear {
  flex: none;
  padding: 0 8rpx;
  font-size: 22rpx;
  color: $text-placeholder;
}

.filter__btn {
  flex: none;
  padding-left: 8rpx;
  font-size: 26rpx;
  color: $primary;
}

.filter__cats {
  width: 100%;
  /* scroll-view 横向滚动需要确定高度，否则部分端会塌陷 */
  height: 76rpx;
  margin-top: 16rpx;
  white-space: nowrap;
}

.filter__cat {
  display: inline-block;
  height: 56rpx;
  padding: 0 28rpx;
  margin: 10rpx 0 0 16rpx;
  font-size: 26rpx;
  line-height: 56rpx;
  color: $text-secondary;
  vertical-align: top;
  background-color: $page-bg;
  border-radius: 28rpx;

  &--active {
    font-weight: 600;
    color: #fff;
    background-color: $primary;
  }

  &:last-child {
    margin-right: 16rpx;
  }
}

.filter__sorts {
  display: flex;
  align-items: center;
  padding: 8rpx 24rpx 12rpx;
  margin-top: 8rpx;
}

.filter__sort {
  margin-right: 40rpx;
  font-size: 26rpx;
  color: $text-secondary;

  &--active {
    font-weight: 600;
    color: $primary;
  }
}

.filter__arrow {
  font-size: 22rpx;
}

.filter__total {
  margin-left: auto;
  font-size: 22rpx;
  color: $text-placeholder;
}

.body {
  padding: 20rpx 24rpx 0;
}

.body__state {
  padding: 120rpx 0;
  font-size: 26rpx;
  color: $text-secondary;
  text-align: center;
}

.grid {
  /*
   * 两列自适应：flex 换行 + 百分比宽度，列宽完全由屏幕宽度按比例算出，不涉及 calc / gap，
   * 避免部分基础库对「calc 内 rpx」取整后总宽超 100% 导致每行只排得下一张卡片。
   * 48.6% × 2 = 97.2%，中间的 2.8% 由 space-between 均分，任何屏幕都是稳定两列。
   */
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.grid__item {
  box-sizing: border-box;
  width: 48.6%;
  margin-bottom: 16rpx;
  min-width: 0;
  overflow: hidden;
}

.foot {
  padding: 32rpx 0 16rpx;
  font-size: 24rpx;
  color: $text-placeholder;
  text-align: center;
}
</style>
