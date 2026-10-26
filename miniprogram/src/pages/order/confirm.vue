<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { addressApi } from '@/api/user'
import { orderApi } from '@/api/order'
import { useCartStore } from '@/store/cart'
import { formatPrice } from '@/utils/format'
import type { Address } from '@/types/api'

const cartStore = useCartStore()
const address = ref<Address | null>(null)
const remark = ref('')
const submitting = ref(false)

const items = computed(() => cartStore.checkedItems)
const total = computed(() => cartStore.checkedAmount)

async function loadAddress() {
  const list = await addressApi.list()
  address.value = list.find((item) => item.isDefault === 1) ?? list[0] ?? null
}

function chooseAddress() {
  uni.navigateTo({ url: '/pages/address/list?select=1' })
}

function onAddressSelected(selected: Address) {
  address.value = selected
}

async function submit() {
  if (!address.value) {
    uni.showToast({ title: '请先选择收货地址', icon: 'none' })
    return
  }
  if (!items.value.length) {
    uni.showToast({ title: '没有可结算的商品', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await orderApi.create(address.value.id, remark.value || undefined)
    cartStore.refreshCount().catch(() => undefined)
    uni.showToast({ title: '下单成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/order/list?status=0' })
    }, 600)
  } finally {
    submitting.value = false
  }
}

onLoad(async () => {
  uni.$on('address:select', onAddressSelected)
  await cartStore.refresh()
  await loadAddress()
})

onUnload(() => {
  uni.$off('address:select', onAddressSelected)
})
</script>

<template>
  <view class="page">
    <view class="address" @tap="chooseAddress">
      <template v-if="address">
        <view class="address__main">
          <view class="address__line">
            <text class="address__receiver">{{ address.receiver }}</text>
            <text class="address__phone">{{ address.phone }}</text>
          </view>
          <text class="address__detail">
            {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detail }}
          </text>
        </view>
      </template>
      <text v-else class="address__empty">+ 请选择收货地址</text>
      <text class="address__arrow">›</text>
    </view>

    <view class="card">
      <EmptyState v-if="!items.length" text="没有可结算的商品" />
      <view v-for="item in items" :key="item.id" class="goods">
        <image class="goods__image" :src="item.mainImage || ''" mode="aspectFill" />
        <view class="goods__body">
          <text class="goods__name ellipsis-2">{{ item.name }}</text>
          <view class="goods__bottom">
            <text class="price">¥{{ formatPrice(item.price) }}</text>
            <text class="goods__qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="cell">
        <text>备注</text>
        <input v-model="remark" class="remark" placeholder="选填，可备注发货要求" placeholder-class="placeholder-muted" />
      </view>
      <view class="cell">
        <text>运费</text>
        <text class="text-26rpx text-muted">包邮</text>
      </view>
    </view>

    <view class="footer safe-bottom">
      <view class="footer__amount">
        <text class="text-24rpx">应付：</text>
        <text class="price-symbol price">¥</text>
        <text class="price text-36rpx">{{ formatPrice(total) }}</text>
      </view>
      <view class="footer__btn" :class="{ 'footer__btn--disabled': submitting }" @tap="submit">
        {{ submitting ? '提交中…' : '提交订单' }}
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding: 20rpx 24rpx 160rpx;
}

.address {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  background-color: #fff;
  border-radius: 20rpx;

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__line {
    display: flex;
    align-items: center;
  }

  &__receiver {
    font-size: 30rpx;
    font-weight: 600;
    margin-right: 16rpx;
  }

  &__phone {
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__detail {
    display: block;
    margin-top: 10rpx;
    font-size: 26rpx;
    color: $text-secondary;
    line-height: 36rpx;
  }

  &__empty {
    flex: 1;
    font-size: 28rpx;
    color: $primary;
  }

  &__arrow {
    font-size: 36rpx;
    color: $text-placeholder;
    margin-left: 12rpx;
  }
}

.card {
  margin-top: 20rpx;
  padding: 0 24rpx;
  background-color: #fff;
  border-radius: 20rpx;
}

.goods {
  display: flex;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $border-color;

  &:last-child {
    border-bottom: none;
  }

  &__image {
    width: 140rpx;
    height: 140rpx;
    border-radius: 12rpx;
    background-color: #f2f2f2;
  }

  &__body {
    flex: 1;
    margin-left: 20rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  &__name {
    font-size: 28rpx;
    line-height: 38rpx;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__qty {
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.remark {
  flex: 1;
  margin-left: 24rpx;
  text-align: right;
  font-size: 26rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 112rpx;
  padding: 0 24rpx;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);

  &__amount {
    display: flex;
    align-items: baseline;
  }

  &__btn {
    padding: 0 52rpx;
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
