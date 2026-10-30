<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { goodsApi } from '@/api/goods'
import { commentApi } from '@/api/comment'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'
import { navIconUri } from '@/utils/nav-icon-uri'
import type { Goods, GoodsCommentPage } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { theme, themePageStyle } = useTheme()

const goods = ref<Goods | null>(null)
const goodsId = ref<number>(0)
const current = ref(0)

/** 评价数据：评分概览 + 前 3 条，空对象表示还没拿到 */
const comments = ref<GoodsCommentPage | null>(null)
const commentPreview = computed(() => comments.value?.page.records ?? [])
const commentCount = computed(() => Number(comments.value?.commentCount ?? 0))
const avgScoreText = computed(() => Number(comments.value?.avgScore ?? 0).toFixed(1))

const cartStore = useCartStore()
const userStore = useUserStore()

const banners = computed(() => {
  if (!goods.value) return []
  const list = (goods.value.images || '').split(',').map((item) => item.trim()).filter(Boolean)
  return list.length ? list : [goods.value.mainImage]
})

/** 售罄：库存为 0 时两个动作按钮统一置灰，避免点了没反馈 */
const soldOut = computed(() => !goods.value || Number(goods.value.stock || 0) <= 0)

/** 购物车图标用 lucide 线性图标按主题次文字色渲染，避免 emoji 在安卓端显示成彩色图形 */
const cartIcon = computed(() => navIconUri('shopping-cart', theme.value.subTextColor))

const cartBadge = computed(() => (cartStore.count > 99 ? '99+' : String(cartStore.count)))

async function load() {
  goods.value = await goodsApi.detail(goodsId.value)
  uni.setNavigationBarTitle({ title: goods.value.name })
}

/** 评价加载失败不打断商品浏览，卡片保持「暂无评价」 */
async function loadComments() {
  try {
    comments.value = await commentApi.goodsComments(goodsId.value, 1, 3)
  } catch {
    comments.value = null
  }
}

function previewImage(urls: string[], index: number) {
  uni.previewImage({ urls, current: urls[index] })
}

async function requireLogin(): Promise<boolean> {
  if (userStore.isLogin) return true
  uni.navigateTo({ url: '/pages/login/index' })
  return false
}

async function addToCart() {
  if (soldOut.value) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return
  }
  if (!(await requireLogin())) return
  await cartStore.add(goodsId.value, 1)
  uni.showToast({ title: '已加入购物车', icon: 'none' })
}

async function buyNow() {
  if (soldOut.value) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return
  }
  if (!(await requireLogin())) return
  // 立即购买走独立直接下单链路：只结算当前商品，不往购物车加记录
  uni.navigateTo({ url: `/pages/order/confirm?mode=buyNow&goodsId=${goodsId.value}&quantity=1` })
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/index' })
}

onLoad((options) => {
  goodsId.value = Number(options?.id ?? 0)
  if (goodsId.value) {
    load()
    // 评价与商品详情并行拉取，互不阻塞
    loadComments()
  }
  // 已登录时同步一次购物车数量，底部角标才有值（失败不打断页面）
  if (userStore.isLogin) {
    cartStore.refreshCount().catch(() => undefined)
  }
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view v-if="goods" class="page">
    <swiper class="banner" :current="current" circular @change="(e) => (current = e.detail.current)">
      <swiper-item v-for="(image, index) in banners" :key="index">
        <image class="banner__image" :src="image" mode="aspectFill" />
      </swiper-item>
    </swiper>

    <view class="info">
      <view class="flex items-baseline">
        <text class="price-symbol price">¥</text>
        <text class="price text-48rpx">{{ formatPrice(goods.price) }}</text>
        <text v-if="goods.originalPrice" class="price-origin">¥{{ formatPrice(goods.originalPrice) }}</text>
      </view>
      <text class="info__name">{{ goods.name }}</text>
      <text v-if="goods.subtitle" class="info__subtitle">{{ goods.subtitle }}</text>
      <view class="info__meta">
        <text>库存 {{ goods.stock }}</text>
        <text>已售 {{ goods.sales || 0 }}</text>
        <text>{{ goods.unit || '件' }}</text>
      </view>
    </view>

    <view class="comments">
      <view class="comments__head">
        <text class="comments__title">商品评价（{{ commentCount }}）</text>
        <view v-if="commentCount > 0" class="comments__score">
          <text class="comments__star">★</text>
          <text class="comments__avg">{{ avgScoreText }}</text>
        </view>
      </view>

      <view v-if="!commentPreview.length" class="comments__empty">暂无评价，期待你的分享</view>

      <view v-for="item in commentPreview" :key="item.id" class="comment">
        <view class="comment__head">
          <text class="comment__user">{{ item.nickname }}</text>
          <view class="comment__stars">
            <text
              v-for="star in 5"
              :key="star"
              class="comment__star"
              :class="{ 'comment__star--on': star <= item.score }"
            >
              ★
            </text>
          </view>
          <text class="comment__time">{{ item.commentedAt.slice(0, 10) }}</text>
        </view>

        <text v-if="item.content" class="comment__content">{{ item.content }}</text>

        <view v-if="item.images.length" class="comment__images">
          <image
            v-for="(image, index) in item.images"
            :key="image"
            class="comment__image"
            :src="image"
            mode="aspectFill"
            @tap="previewImage(item.images, index)"
          />
        </view>

        <view v-if="item.reply" class="comment__reply">
          <text class="comment__reply-label">商家回复：</text>
          <text class="comment__reply-text">{{ item.reply }}</text>
        </view>
      </view>
    </view>

    <view class="detail">
      <text class="detail__title">商品详情</text>
      <rich-text v-if="goods.detail" class="detail__content" :nodes="goods.detail" />
      <text v-else class="detail__empty">暂无详情</text>
    </view>

    <view class="bar">
      <view class="bar__cart" hover-class="bar__cart--hover" :hover-stay-time="80" @tap="goCart">
        <view class="bar__cart-icon-wrap">
          <image class="bar__cart-icon" :src="cartIcon" mode="aspectFit" />
          <text v-if="cartStore.count > 0" class="bar__cart-badge">{{ cartBadge }}</text>
        </view>
        <text class="bar__cart-label">购物车</text>
      </view>
      <view class="bar__actions">
        <view
          class="bar__btn bar__btn--cart"
          :class="{ 'bar__btn--disabled': soldOut }"
          hover-class="bar__btn--hover"
          :hover-stay-time="80"
          @tap="addToCart"
        >
          加入购物车
        </view>
        <view
          class="bar__btn bar__btn--buy"
          :class="{ 'bar__btn--disabled': soldOut }"
          hover-class="bar__btn--hover"
          :hover-stay-time="80"
          @tap="buyNow"
        >
          立即购买
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  /* 给底部操作栏（84rpx 按钮 + 上下 16rpx 内边距 + 安全区）留位 */
  padding-bottom: 180rpx;
}

.banner {
  width: 100%;
  height: 750rpx;
  background-color: #f2f2f2;

  &__image {
    width: 100%;
    height: 100%;
  }
}

.info {
  padding: 28rpx 24rpx;
  background-color: #fff;

  &__name {
    display: block;
    margin-top: 16rpx;
    font-size: 34rpx;
    font-weight: 600;
    line-height: 46rpx;
  }

  &__subtitle {
    display: block;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__meta {
    display: flex;
    gap: 32rpx;
    margin-top: 20rpx;
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.comments {
  margin-top: 20rpx;
  padding: 24rpx;
  background-color: #fff;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__score {
    display: flex;
    align-items: center;
  }

  &__star {
    font-size: 26rpx;
    color: #ffb400;
  }

  &__avg {
    margin-left: 6rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: #ffb400;
  }

  &__empty {
    display: block;
    margin-top: 20rpx;
    font-size: 26rpx;
    color: $text-placeholder;
  }
}

.comment {
  padding: 20rpx 0;

  & + & {
    border-top: 1rpx solid $border-color;
  }

  &__head {
    display: flex;
    align-items: center;
  }

  &__user {
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__stars {
    display: flex;
    margin-left: 12rpx;
  }

  &__star {
    font-size: 22rpx;
    color: #dcdcdc;

    &--on {
      color: #ffb400;
    }
  }

  &__time {
    margin-left: auto;
    font-size: 22rpx;
    color: $text-placeholder;
  }

  &__content {
    display: block;
    margin-top: 12rpx;
    font-size: 26rpx;
    line-height: 38rpx;
    color: #434343;
  }

  &__images {
    display: flex;
    gap: 12rpx;
    margin-top: 12rpx;
  }

  &__image {
    width: 150rpx;
    height: 150rpx;
    border-radius: 12rpx;
    background-color: #f2f2f2;
  }

  &__reply {
    margin-top: 12rpx;
    padding: 12rpx 16rpx;
    background-color: #f8f8f8;
    border-radius: 12rpx;
  }

  &__reply-label {
    font-size: 24rpx;
    color: $primary;
  }

  &__reply-text {
    font-size: 24rpx;
    color: $text-secondary;
  }
}

.detail {
  margin-top: 20rpx;
  padding: 24rpx;
  background-color: #fff;

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__content {
    display: block;
    margin-top: 20rpx;
    font-size: 28rpx;
    line-height: 44rpx;
    color: #434343;
  }

  &__empty {
    display: block;
    margin-top: 20rpx;
    font-size: 26rpx;
    color: $text-placeholder;
  }
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  /* 底部安全区自己算，避免与全局 .safe-bottom 的 padding-bottom 互相覆盖 */
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);

  &__cart {
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 96rpx;
    transition: opacity 0.15s;

    &--hover {
      opacity: 0.6;
    }
  }

  &__cart-icon-wrap {
    position: relative;
    width: 46rpx;
    height: 46rpx;
  }

  &__cart-icon {
    width: 46rpx;
    height: 46rpx;
  }

  &__cart-badge {
    position: absolute;
    top: -12rpx;
    right: -20rpx;
    box-sizing: border-box;
    min-width: 30rpx;
    height: 30rpx;
    padding: 0 6rpx;
    border-radius: 15rpx;
    background-color: $price;
    color: #fff;
    font-size: 20rpx;
    line-height: 30rpx;
    text-align: center;
  }

  &__cart-label {
    margin-top: 4rpx;
    font-size: 20rpx;
    line-height: 24rpx;
    color: $text-secondary;
  }

  &__actions {
    flex: 1;
    display: flex;
    align-items: center;
  }

  &__btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 84rpx;
    border-radius: 42rpx;
    font-size: 30rpx;
    font-weight: 600;
    letter-spacing: 1rpx;
    transition: opacity 0.15s, transform 0.15s;
  }

  /* 次要动作：浅主色底 + 1rpx 主色描边，白底上也能看出可点边界 */
  &__btn--cart {
    margin-right: 20rpx;
    color: $primary;
    background-color: $primary-light;
    box-shadow: inset 0 0 0 1rpx $primary;
  }

  /* 主要动作：主色实心 + 轻微投影，形成明确的主次层级 */
  &__btn--buy {
    color: #fff;
    background-color: $primary;
    box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.12);
  }

  &__btn--hover {
    opacity: 0.82;
    transform: scale(0.985);
  }

  /* 声明在最后：同权重下覆盖上面的实心 / 描边样式 */
  &__btn--disabled {
    color: #fff;
    background-color: #d9d9d9;
    box-shadow: none;
  }
}
</style>
