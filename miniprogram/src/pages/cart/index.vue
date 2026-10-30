<script setup lang="ts">
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import EmptyState from '@/components/empty-state.vue'
import { useCartStore } from '@/store/cart'
import { useDecorateStore } from '@/store/decorate'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const cartStore = useCartStore()
const userStore = useUserStore()
const decorateStore = useDecorateStore()

const canCheckout = computed(() => cartStore.checkedItems.length > 0)

async function load() {
  if (!userStore.isLogin) {
    return
  }
  await cartStore.refresh()
}

async function changeQuantity(id: number, quantity: number, stock: number) {
  if (quantity < 1) {
    return
  }
  if (quantity > stock) {
    uni.showToast({ title: '超出库存', icon: 'none' })
    return
  }
  await cartStore.updateQuantity(id, quantity)
}

async function removeItem(id: number) {
  const res = await uni.showModal({ title: '提示', content: '确定删除该商品吗？' })
  if (res.confirm) {
    closeSwipe()
    await cartStore.remove(id)
  }
}

/* 左滑删除：同一时刻最多展开一项，滑动中跟手、松手吸附 */
const DELETE_WIDTH_PX = uni.upx2px(140) // 对应样式里的删除按钮宽度 140rpx
const swipeState = reactive({
  openId: 0, // 当前展开的购物车项
  touchingId: 0, // 正在触摸的购物车项
  startX: 0,
  startY: 0,
  moveX: 0, // 本次触摸的横向位移（px，负值向左）
})

function closeSwipe() {
  swipeState.openId = 0
  swipeState.touchingId = 0
  swipeState.moveX = 0
}

function swipeStyle(id: number) {
  if (swipeState.touchingId === id) {
    return { transform: `translateX(${swipeState.moveX}px)` }
  }
  return { transform: swipeState.openId === id ? `translateX(-${DELETE_WIDTH_PX}px)` : 'translateX(0px)' }
}

function onTouchStart(id: number, e: TouchEvent) {
  const touch = e.touches?.[0]
  if (!touch) return
  swipeState.touchingId = id
  swipeState.startX = touch.clientX
  swipeState.startY = touch.clientY
  swipeState.moveX = 0
}

function onTouchMove(id: number, e: TouchEvent) {
  if (swipeState.touchingId !== id) return
  const touch = e.touches?.[0]
  if (!touch) return
  const dx = touch.clientX - swipeState.startX
  const dy = touch.clientY - swipeState.startY
  // 垂直滑动优先交给页面滚动
  if (Math.abs(dy) > Math.abs(dx)) return
  // 基于当前展开状态做增量位移：已展开的继续左滑不再移动，右滑可收回
  const base = swipeState.openId === id ? -DELETE_WIDTH_PX : 0
  swipeState.moveX = Math.max(Math.min(base + dx, 0), -DELETE_WIDTH_PX)
}

function onTouchEnd(id: number) {
  if (swipeState.touchingId !== id) return
  const moved = Math.abs(swipeState.moveX)
  // 位移超过一半（或快速轻扫）即展开，否则回弹
  if (swipeState.moveX <= -DELETE_WIDTH_PX / 2) {
    swipeState.openId = id
  } else if (swipeState.openId === id && moved < 6) {
    // 已展开状态下轻点内容区则收起
    swipeState.openId = 0
  }
  swipeState.touchingId = 0
  swipeState.moveX = 0
}

function toDetail(item: { id: number, goodsId: number }) {
  // 展开状态下点击内容区只收起，不跳详情
  if (swipeState.openId === item.id) {
    closeSwipe()
    return
  }
  uni.navigateTo({ url: `/pages/goods/detail?id=${item.goodsId}` })
}

function toLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

function checkout() {
  uni.navigateTo({ url: '/pages/order/confirm' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function onCheckout() {
  if (!canCheckout.value) {
    uni.showToast({ title: '请选择要结算的商品', icon: 'none' })
    return
  }
  checkout()
}

onShow(() => {
  load()
  decorateStore.ensureNav()
  decorateStore.ensureTheme()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page">
    <view v-if="!userStore.isLogin" class="login-tip">
      <text class="login-tip__text">登录后查看购物车</text>
      <view class="login-tip__btn" @tap="toLogin">去登录</view>
    </view>

    <template v-else>
      <EmptyState v-if="!cartStore.items.length" text="购物车还是空的" tip="去挑几件喜欢的商品吧">
        <view class="empty-btn" @tap="goHome">去逛逛</view>
      </EmptyState>

      <template v-else>
        <view class="list">
          <view v-for="item in cartStore.items" :key="item.id" class="swipe">
            <view
              class="swipe__content"
              :class="{ 'swipe__content--anim': swipeState.touchingId !== item.id }"
              :style="swipeStyle(item.id)"
              @touchstart="onTouchStart(item.id, $event)"
              @touchmove="onTouchMove(item.id, $event)"
              @touchend="onTouchEnd(item.id)"
              @touchcancel="onTouchEnd(item.id)"
            >
              <view class="item" :class="{ 'item--invalid': item.invalid }">
                <view
                  class="item__check"
                  :class="{ 'item__check--on': item.checked === 1 }"
                  @tap="cartStore.toggleChecked(item.id, item.checked !== 1)"
                />
                <image class="item__image" :src="item.mainImage || ''" mode="aspectFill" @tap="toDetail(item)" />
                <view class="item__body">
                  <text class="item__name ellipsis-2" @tap="toDetail(item)">{{ item.name }}</text>
                  <text v-if="item.invalid" class="item__invalid-tip">商品已下架或库存不足</text>
                  <view class="item__bottom">
                    <view class="flex items-baseline">
                      <text class="price-symbol price">¥</text>
                      <text class="price text-30rpx">{{ formatPrice(item.price) }}</text>
                    </view>
                    <view class="stepper">
                      <text class="stepper__btn" @tap="changeQuantity(item.id, item.quantity - 1, item.stock)">-</text>
                      <text class="stepper__value">{{ item.quantity }}</text>
                      <text class="stepper__btn" @tap="changeQuantity(item.id, item.quantity + 1, item.stock)">+</text>
                    </view>
                  </view>
                </view>
              </view>
            </view>
            <view class="swipe__delete" @tap="removeItem(item.id)">删除</view>
          </view>
        </view>

        <view class="settle safe-bottom">
          <view class="settle__all" @tap="cartStore.toggleAll(!cartStore.allChecked)">
            <view class="item__check" :class="{ 'item__check--on': cartStore.allChecked }" />
            <text class="settle__all-text">全选</text>
          </view>
          <view class="settle__right">
            <view class="settle__amount">
              <text class="text-24rpx">合计：</text>
              <text class="price-symbol price">¥</text>
              <text class="price text-34rpx">{{ formatPrice(cartStore.checkedAmount) }}</text>
            </view>
            <view class="settle__btn" :class="{ 'settle__btn--disabled': !canCheckout }" @tap="onCheckout">
              结算({{ cartStore.checkedItems.length }})
            </view>
          </view>
        </view>
      </template>
    </template>

    <DecorateTabbar :config="decorateStore.nav.tabbar" current="/pages/cart/index" />
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 260rpx;
}

.login-tip {
  margin: 120rpx 48rpx;
  padding: 56rpx 32rpx;
  background-color: #fff;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__text {
    font-size: 28rpx;
    color: $text-secondary;
  }

  &__btn {
    margin-top: 28rpx;
    padding: 16rpx 56rpx;
    background-color: $primary;
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
  }
}

.empty-btn {
  margin-top: 32rpx;
  padding: 16rpx 56rpx;
  background-color: $primary;
  color: #fff;
  border-radius: 40rpx;
  font-size: 26rpx;
}

.list {
  margin: 20rpx 24rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 0 20rpx;
}

.swipe {
  position: relative;
  overflow: hidden;
  border-bottom: 1rpx solid $border-color;

  &:last-child {
    border-bottom: none;
  }

  &__content {
    position: relative;
    z-index: 1;
    background-color: #fff;

    /* 松手后的展开/回弹动画；跟手滑动时关闭避免迟滞 */
    &--anim {
      transition: transform 0.2s ease-out;
    }
  }

  &__delete {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    width: 140rpx;
    background-color: $danger;
    color: #fff;
    font-size: 26rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  position: relative;

  &--invalid {
    opacity: 0.6;
  }

  &__check {
    width: 36rpx;
    height: 36rpx;
    border-radius: 50%;
    border: 2rpx solid #d9d9d9;
    flex-shrink: 0;

    &--on {
      background-color: $primary;
      border-color: $primary;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        left: 11rpx;
        top: 6rpx;
        width: 8rpx;
        height: 16rpx;
        border-right: 3rpx solid #fff;
        border-bottom: 3rpx solid #fff;
        transform: rotate(45deg);
      }
    }
  }

  &__image {
    width: 160rpx;
    height: 160rpx;
    border-radius: 12rpx;
    margin: 0 20rpx;
    background-color: #f2f2f2;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
    height: 160rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    line-height: 38rpx;
  }

  &__invalid-tip {
    font-size: 22rpx;
    color: $danger;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.stepper {
  display: flex;
  align-items: center;

  &__btn {
    width: 48rpx;
    height: 48rpx;
    line-height: 44rpx;
    text-align: center;
    border: 1rpx solid $border-color;
    border-radius: 8rpx;
    font-size: 28rpx;
    color: $text-secondary;
  }

  &__value {
    min-width: 60rpx;
    text-align: center;
    font-size: 26rpx;
  }
}

.settle {
  position: fixed;
  left: 0;
  right: 0;
  /* 让开底部自定义 TabBar */
  bottom: 104rpx;
  height: 112rpx;
  padding: 0 24rpx;
  background-color: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__all {
    display: flex;
    align-items: center;
  }

  &__all-text {
    margin-left: 12rpx;
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__right {
    display: flex;
    align-items: center;
  }

  &__amount {
    display: flex;
    align-items: baseline;
    margin-right: 20rpx;
  }

  &__btn {
    padding: 0 44rpx;
    height: 76rpx;
    line-height: 76rpx;
    background-color: $primary;
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;

    &--disabled {
      background-color: #d9d9d9;
    }
  }
}
</style>
