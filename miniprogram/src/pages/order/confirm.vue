<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { addressApi } from '@/api/user'
import { goodsApi } from '@/api/goods'
import { orderApi } from '@/api/order'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'
import type { Address, CartItem, FreightPreview, Goods, Order } from '@/types/api'
import type { UsableCoupon } from '@/types/coupon'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

const cartStore = useCartStore()
const userStore = useUserStore()
const address = ref<Address | null>(null)
const remark = ref('')
const submitting = ref(false)

/** 运费：随收货地址联动，服务端下单时按同一口径重算 */
const freight = ref<FreightPreview | null>(null)

/** 立即购买模式：mode=buyNow，只结算指定商品，不读购物车 */
const isBuyNow = ref(false)
const buyNowGoods = ref<Goods | null>(null)
const buyNowQuantity = ref(1)

/** 结算页可用优惠券：服务端按购物车已勾选商品（或立即购买商品）实时计算 */
const coupons = ref<UsableCoupon[]>([])
const selectedCouponUserId = ref<number | null>(null)
const couponPickerVisible = ref(false)

const items = computed<CartItem[]>(() => {
  if (isBuyNow.value && buyNowGoods.value) {
    return [{
      id: buyNowGoods.value.id,
      goodsId: buyNowGoods.value.id,
      name: buyNowGoods.value.name,
      mainImage: buyNowGoods.value.mainImage,
      price: buyNowGoods.value.price,
      stock: buyNowGoods.value.stock,
      quantity: buyNowQuantity.value,
      checked: 1,
      subtotal: Number(buyNowGoods.value.price) * buyNowQuantity.value,
      invalid: false
    }]
  }
  return cartStore.checkedItems
})
const total = computed(() => {
  if (isBuyNow.value && buyNowGoods.value) {
    return Number(buyNowGoods.value.price) * buyNowQuantity.value
  }
  return cartStore.checkedAmount
})

const selectedCoupon = computed(
  () => coupons.value.find((item) => item.couponUserId === selectedCouponUserId.value && item.usable) ?? null
)
const discount = computed(() => selectedCoupon.value?.discountAmount ?? 0)

/** 会员等级折扣百分比（100 表示无折扣），来源为 /user/profile */
const levelDiscount = computed(() => Number(userStore.profile?.levelDiscount ?? 100))
const levelName = computed(() => userStore.profile?.levelName ?? '')

/**
 * 会员折扣金额：与后端同口径（商品金额 × (100 - 折扣) / 100，四舍五入到分）。
 * 本地只用于展示，下单时由服务端按同一公式重算并落库。
 */
const memberDiscount = computed(() => {
  const rate = levelDiscount.value
  if (!(rate > 0) || rate >= 100) return 0
  return Math.round(total.value * (100 - rate)) / 100
})
/** 折后商品金额：券门槛、运费包邮门槛、应付金额都基于它计算 */
const discountedTotal = computed(() => Math.max(total.value - memberDiscount.value, 0))

const freightAmount = computed(() =>
  freight.value?.deliverable ? Number(freight.value.freightAmount ?? 0) : 0
)
const notDeliverable = computed(() => freight.value !== null && !freight.value.deliverable)
const payAmount = computed(() => Math.max(discountedTotal.value - discount.value, 0) + freightAmount.value)
/** 会员折扣文案：98 -> 9.8折、90 -> 9折；无折扣返回空串 */
const levelText = computed(() => {
  const rate = levelDiscount.value
  const text = rate > 0 && rate < 100 ? `${(rate / 10).toFixed(rate % 10 === 0 ? 0 : 1)}折` : ''
  return [levelName.value, text].filter(Boolean).join(' ')
})

/** 合计优惠：会员折扣 + 优惠券 */
const savedTotal = computed(() => memberDiscount.value + discount.value)
const usableCount = computed(() => coupons.value.filter((item) => item.usable).length)

async function loadFreight() {
  if (!address.value) {
    freight.value = null
    return
  }
  try {
    // 运费按折后商品金额计算，与下单口径一致（满额包邮门槛看折后金额）
    freight.value = await orderApi.freightPreview(address.value.id, discountedTotal.value)
  } catch {
    freight.value = null
  }
}

async function loadAddress() {
  const list = await addressApi.list()
  address.value = list.find((item) => item.isDefault === 1) ?? list[0] ?? null
  await loadFreight()
}

async function loadCoupons() {
  try {
    // 立即购买按单个商品金额算券，购物车结算按勾选商品金额算券
    const params = isBuyNow.value && buyNowGoods.value
      ? { goodsId: buyNowGoods.value.id, quantity: buyNowQuantity.value }
      : undefined
    coupons.value = await orderApi.usableCoupons(params)
    // 默认选中抵扣最大的可用券（接口已按可用优先、抵扣从大到小排序）
    selectedCouponUserId.value = coupons.value.find((item) => item.usable)?.couponUserId ?? null
  } catch {
    coupons.value = []
    selectedCouponUserId.value = null
  }
}

function chooseAddress() {
  uni.navigateTo({ url: '/pages/address/list?select=1' })
}

function onAddressSelected(selected: Address) {
  address.value = selected
  loadFreight()
}

function onPickCoupon(coupon: UsableCoupon) {
  if (!coupon.usable) {
    uni.showToast({ title: coupon.reason || '该券不可用', icon: 'none' })
    return
  }
  // 再点一次已选中的券 = 取消使用
  selectedCouponUserId.value =
    selectedCouponUserId.value === coupon.couponUserId ? null : coupon.couponUserId
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
  if (notDeliverable.value) {
    uni.showToast({ title: '该地区暂不提供配送，请更换收货地址', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    let created: Order | undefined
    if (isBuyNow.value && buyNowGoods.value) {
      created = await orderApi.buyNow({
        goodsId: buyNowGoods.value.id,
        quantity: buyNowQuantity.value,
        addressId: address.value.id,
        remark: remark.value || undefined,
        couponUserId: selectedCouponUserId.value ?? undefined
      })
    } else {
      created = await orderApi.create(address.value.id, remark.value || undefined, selectedCouponUserId.value ?? undefined)
    }
    cartStore.refreshCount().catch(() => undefined)
    uni.showToast({ title: '下单成功', icon: 'success' })
    // 下单后直接进这笔订单的详情，用户可以立刻「去支付」，不用再在列表里找
    const target = created?.id ? `/pages/order/detail?id=${created.id}` : '/pages/order/list?status=0'
    setTimeout(() => {
      uni.redirectTo({ url: target })
    }, 600)
  } finally {
    submitting.value = false
  }
}

onLoad(async (options) => {
  uni.$on('address:select', onAddressSelected)
  // 立即购买模式：加载单个商品，不读购物车、不影响购物车勾选状态
  const goodsId = Number(options?.goodsId ?? 0)
  if (options?.mode === 'buyNow' && goodsId) {
    isBuyNow.value = true
    buyNowQuantity.value = Math.max(Number(options?.quantity ?? 1), 1)
    const goods = await goodsApi.detail(goodsId)
    // 已下架商品按空态处理，避免填完信息提交时才被后端拒绝
    if (goods && goods.status === 1) {
      buyNowGoods.value = goods
    }
  } else {
    await cartStore.refresh()
  }
  // 会员折扣依赖等级信息，缺失时补一次（先拿到等级，运费才能按折后金额算）
  if (!userStore.profile) {
    await userStore.fetchProfile().catch(() => undefined)
  }
  await loadAddress()
  await loadCoupons()
})

onUnload(() => {
  uni.$off('address:select', onAddressSelected)
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
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
        <text>商品金额</text>
        <text class="text-26rpx text-muted">¥{{ formatPrice(total) }}</text>
      </view>
      <view v-if="memberDiscount > 0" class="cell">
        <view class="cell__label">
          <text>会员折扣</text>
          <text v-if="levelText" class="cell__tag">{{ levelText }}</text>
        </view>
        <text class="coupon-save">-¥{{ formatPrice(memberDiscount) }}</text>
      </view>
      <view class="cell">
        <text>备注</text>
        <input v-model="remark" class="remark" placeholder="选填，可备注发货要求" placeholder-class="placeholder-muted" />
      </view>
      <view class="cell">
        <text>运费</text>
        <text v-if="notDeliverable" class="freight-warning">该地区暂不配送</text>
        <text v-else-if="freightAmount > 0" class="text-26rpx text-muted">¥{{ formatPrice(freightAmount) }}</text>
        <text v-else class="text-26rpx text-muted">包邮</text>
      </view>
      <view class="cell" @tap="couponPickerVisible = true">
        <text>优惠券</text>
        <view class="cell__right">
          <text v-if="selectedCoupon" class="coupon-save">-¥{{ formatPrice(discount) }}</text>
          <text v-else-if="usableCount" class="coupon-count">{{ usableCount }} 张可用</text>
          <text v-else class="text-26rpx text-muted">暂无可用</text>
          <text class="address__arrow">›</text>
        </view>
      </view>
    </view>

    <view class="footer safe-bottom">
      <view class="footer__amount">
        <text class="text-24rpx">应付：</text>
        <text class="price-symbol price">¥</text>
        <text class="price text-36rpx">{{ formatPrice(payAmount) }}</text>
        <text v-if="savedTotal > 0" class="footer__saved">已优惠 ¥{{ formatPrice(savedTotal) }}</text>
      </view>
      <view class="footer__btn" :class="{ 'footer__btn--disabled': submitting || notDeliverable }" @tap="submit">
        {{ submitting ? '提交中…' : '提交订单' }}
      </view>
    </view>

    <!-- 优惠券选择弹层 -->
    <view v-if="couponPickerVisible" class="coupon-mask" @tap="couponPickerVisible = false">
      <view class="coupon-panel" @tap.stop>
        <view class="coupon-panel__head">
          <text class="coupon-panel__title">选择优惠券</text>
          <text class="coupon-panel__close" @tap="couponPickerVisible = false">✕</text>
        </view>
        <scroll-view scroll-y class="coupon-panel__body">
          <view
            v-for="coupon in coupons"
            :key="coupon.couponUserId"
            class="coupon-item"
            :class="{
              'coupon-item--disabled': !coupon.usable,
              'coupon-item--active': coupon.couponUserId === selectedCouponUserId
            }"
            @tap="onPickCoupon(coupon)"
          >
            <view class="coupon-item__value">
              <text class="coupon-item__amount">{{ coupon.valueText }}</text>
              <text class="coupon-item__rule">{{ coupon.ruleText }}</text>
            </view>
            <view class="coupon-item__info">
              <text class="coupon-item__name ellipsis">{{ coupon.name }}</text>
              <text class="coupon-item__expire">
                {{ coupon.expireAt ? `${coupon.expireAt} 前有效` : '长期有效' }}
              </text>
              <text v-if="!coupon.usable" class="coupon-item__reason">{{ coupon.reason }}</text>
            </view>
            <view class="coupon-item__check">
              <text v-if="coupon.couponUserId === selectedCouponUserId" class="coupon-item__check-icon">✓</text>
            </view>
          </view>
          <EmptyState v-if="!coupons.length" text="暂无可用优惠券，去领券中心逛逛吧" />
        </scroll-view>
        <view class="coupon-panel__foot" @tap="couponPickerVisible = false">完成</view>
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

.cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $border-color;

  &:last-child {
    border-bottom: none;
  }

  &__right {
    display: flex;
    align-items: center;
  }

  &__label {
    display: flex;
    align-items: center;
  }

  &__tag {
    margin-left: 12rpx;
    padding: 2rpx 12rpx;
    font-size: 20rpx;
    color: $price;
    background-color: rgba(255, 90, 31, 0.1);
    border-radius: 6rpx;
  }
}

.coupon-save {
  font-size: 28rpx;
  font-weight: 600;
  color: $price;
}

.coupon-count {
  font-size: 26rpx;
  color: $primary;
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

  &__saved {
    margin-left: 12rpx;
    font-size: 22rpx;
    color: $price;
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

.freight-warning {
  font-size: 26rpx;
  color: $danger;
}

/* 优惠券选择弹层 */
.coupon-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  background-color: rgba(0, 0, 0, 0.5);
}

.coupon-panel {
  width: 100%;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx 32rpx 20rpx;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__close {
    font-size: 30rpx;
    color: $text-secondary;
    padding: 0 8rpx;
  }

  &__body {
    flex: 1;
    min-height: 0;
    max-height: 52vh;
    padding: 0 24rpx;
    box-sizing: border-box;
  }

  &__foot {
    padding: 24rpx;
    padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
    font-size: 30rpx;
    font-weight: 600;
    color: #fff;
    text-align: center;
    background-color: $primary;
  }
}

.coupon-item {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  padding: 24rpx;
  border: 2rpx solid $border-color;
  border-radius: 16rpx;

  &:first-child {
    margin-top: 0;
  }

  &--active {
    border-color: $primary;
    background-color: rgba(255, 90, 31, 0.04);
  }

  &--disabled {
    opacity: 0.55;
  }

  &__value {
    display: flex;
    flex: none;
    flex-direction: column;
    align-items: center;
    width: 160rpx;
    color: $price;
  }

  &__amount {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__rule {
    margin-top: 6rpx;
    font-size: 20rpx;
    color: $text-secondary;
  }

  &__info {
    flex: 1;
    min-width: 0;
    margin-left: 20rpx;
  }

  &__name {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
  }

  &__expire {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $text-secondary;
  }

  &__reason {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $price;
  }

  &__check {
    flex: none;
    width: 48rpx;
    text-align: center;
  }

  &__check-icon {
    font-size: 32rpx;
    font-weight: 700;
    color: $primary;
  }
}
</style>
