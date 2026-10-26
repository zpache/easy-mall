<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'

const cartStore = useCartStore()
const userStore = useUserStore()

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
    await cartStore.remove(id)
  }
}

function toLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

function checkout() {
  uni.navigateTo({ url: '/pages/order/confirm' })
}

function toDetail(goodsId: number) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${goodsId}` })
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
})
</script>

<template>
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
          <view v-for="item in cartStore.items" :key="item.id" class="item" :class="{ 'item--invalid': item.invalid }">
            <view
              class="item__check"
              :class="{ 'item__check--on': item.checked === 1 }"
              @tap="cartStore.toggleChecked(item.id, item.checked !== 1)"
            />
            <image class="item__image" :src="item.mainImage || ''" mode="aspectFill" @tap="toDetail(item.goodsId)" />
            <view class="item__body">
              <text class="item__name ellipsis-2">{{ item.name }}</text>
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
            <text class="item__delete" @tap="removeItem(item.id)">删除</text>
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
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding-bottom: 140rpx;
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

.item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $border-color;
  position: relative;

  &:last-child {
    border-bottom: none;
  }

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

  &__delete {
    position: absolute;
    right: 0;
    bottom: 24rpx;
    font-size: 22rpx;
    color: $text-placeholder;
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
  bottom: 0;
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
