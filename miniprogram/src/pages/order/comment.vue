<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import { orderApi } from '@/api/order'
import { commentApi } from '@/api/comment'
import { uploadCommentImage } from '@/api/file'
import { formatPrice } from '@/utils/format'
import type { Order } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

/** 单个商品的评价草稿 */
interface Draft {
  goodsId: number
  goodsName: string
  goodsImage: string
  price: number
  score: number
  content: string
  images: string[]
  uploading: boolean
}

const MAX_IMAGES = 3
const MAX_CONTENT = 500

const orderId = ref(0)
const loading = ref(true)
const submitting = ref(false)
const order = ref<Order | null>(null)
const drafts = ref<Draft[]>([])

const isEmpty = computed(() => !loading.value && drafts.value.length === 0)
const uploading = computed(() => drafts.value.some((draft) => draft.uploading))

onLoad(async (options) => {
  orderId.value = Number(options?.id ?? 0)
  if (!orderId.value) {
    loading.value = false
    return
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    const data = await orderApi.detail(orderId.value)
    order.value = data
    // 已评价过的商品不再出现在待评价列表里
    drafts.value = data.items
      .filter((item) => !item.commented)
      .map((item) => ({
        goodsId: item.goodsId,
        goodsName: item.goodsName,
        goodsImage: item.goodsImage,
        price: item.price,
        score: 5,
        content: '',
        images: [],
        uploading: false
      }))
  } finally {
    loading.value = false
  }
}

function toast(title: string) {
  uni.showToast({ title, icon: 'none' })
}

function setScore(draft: Draft, score: number) {
  draft.score = score
}

/** 选图后逐张上传，拿到服务端地址再入草稿，避免提交时还在传 */
async function chooseImages(draft: Draft) {
  const remain = MAX_IMAGES - draft.images.length
  if (remain <= 0) {
    toast(`最多上传 ${MAX_IMAGES} 张`)
    return
  }
  let paths: string[] = []
  try {
    const res = await uni.chooseImage({
      count: remain,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera']
    })
    paths = (res.tempFilePaths ?? []) as string[]
  } catch {
    return
  }
  if (!paths.length) return

  draft.uploading = true
  try {
    for (const path of paths) {
      if (draft.images.length >= MAX_IMAGES) break
      const url = await uploadCommentImage(path)
      draft.images.push(url)
    }
  } catch {
    // uploadCommentImage 内部已提示
  } finally {
    draft.uploading = false
  }
}

function removeImage(draft: Draft, index: number) {
  draft.images.splice(index, 1)
}

async function submit() {
  if (submitting.value || uploading.value) return
  const items = drafts.value
  if (!items.length) {
    toast('没有待评价的商品')
    return
  }
  if (items.some((draft) => draft.content.length > MAX_CONTENT)) {
    toast(`评价内容最多 ${MAX_CONTENT} 字`)
    return
  }

  submitting.value = true
  try {
    await commentApi.submit(
      orderId.value,
      items.map((draft) => ({
        goodsId: draft.goodsId,
        score: draft.score,
        content: draft.content.trim() || undefined,
        images: draft.images
      }))
    )
    uni.showToast({ title: '评价已提交，审核通过后展示', icon: 'none' })
    // 通知订单列表/详情刷新，回到上一页即可看到已评价状态
    uni.$emit('order:refresh', { orderId: orderId.value })
    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (e) {
    const message = (e as Error)?.message
    if (message) toast(message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page" :class="{ 'page--with-bar': !isEmpty }">
    <view v-if="loading" class="loading">加载中…</view>

    <EmptyState
      v-else-if="isEmpty"
      text="没有待评价的商品"
      tip="订单已完成且尚未评价的商品会出现在这里"
    />

    <template v-else>
      <view v-if="order" class="tip">订单 {{ order.orderNo }} · 评价提交后需审核，通过后才会展示给其他买家</view>

      <view v-for="(draft, index) in drafts" :key="draft.goodsId" class="card">
        <view class="goods">
          <image class="goods__image" :src="draft.goodsImage" mode="aspectFill" />
          <view class="goods__body">
            <text class="goods__name ellipsis-2">{{ draft.goodsName }}</text>
            <text class="goods__price">¥{{ formatPrice(draft.price) }}</text>
          </view>
        </view>

        <view class="score">
          <text class="score__label">商品评分</text>
          <view class="score__stars">
            <text
              v-for="star in 5"
              :key="star"
              class="score__star"
              :class="{ 'score__star--on': star <= draft.score }"
              @tap="setScore(draft, star)"
            >
              ★
            </text>
          </view>
          <text class="score__text">{{ draft.score }} 分</text>
        </view>

        <view class="field">
          <textarea
            v-model="draft.content"
            class="field__textarea"
            :maxlength="MAX_CONTENT"
            placeholder="说说这款商品的体验，例如口感、包装、物流…"
            placeholder-class="field__placeholder"
          />
          <text class="field__count">{{ draft.content.length }}/{{ MAX_CONTENT }}</text>
        </view>

        <view class="images">
          <view v-for="(image, imageIndex) in draft.images" :key="image" class="images__item">
            <image class="images__pic" :src="image" mode="aspectFill" />
            <view class="images__remove" @tap.stop="removeImage(draft, imageIndex)">×</view>
          </view>
          <view
            v-if="draft.images.length < MAX_IMAGES"
            class="images__add"
            @tap="chooseImages(draft)"
          >
            <text class="images__plus">+</text>
            <text class="images__hint">{{ draft.uploading ? '上传中' : '晒图' }}</text>
          </view>
        </view>
      </view>

      <view class="bar">
        <view
          class="bar__btn bar__btn--primary"
          :class="{ 'bar__btn--disabled': submitting || uploading }"
          hover-class="bar__btn--hover"
          :hover-stay-time="80"
          @tap="submit"
        >
          {{ submitting ? '提交中…' : '提交评价' }}
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding: 20rpx 24rpx 40rpx;

  &--with-bar {
    padding-bottom: 200rpx;
  }
}

.loading {
  padding: 160rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: $text-placeholder;
}

.tip {
  padding: 8rpx 4rpx 20rpx;
  font-size: 24rpx;
  color: $text-secondary;
}

.card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;
}

.goods {
  display: flex;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid $border-color;

  &__image {
    width: 110rpx;
    height: 110rpx;
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

  &__price {
    font-size: 26rpx;
    color: $primary;
  }
}

.score {
  display: flex;
  align-items: center;
  padding: 20rpx 0;

  &__label {
    font-size: 26rpx;
    color: $text-secondary;
  }

  &__stars {
    display: flex;
    margin-left: 16rpx;
  }

  &__star {
    padding: 0 6rpx;
    font-size: 40rpx;
    line-height: 40rpx;
    color: #dcdcdc;

    &--on {
      color: #ffb400;
    }
  }

  &__text {
    margin-left: 12rpx;
    font-size: 24rpx;
    color: $text-placeholder;
  }
}

.field {
  position: relative;
  padding: 20rpx;
  background-color: #f8f8f8;
  border-radius: 16rpx;

  &__textarea {
    width: 100%;
    height: 160rpx;
    font-size: 26rpx;
    line-height: 38rpx;
  }

  &__placeholder {
    color: $text-placeholder;
  }

  &__count {
    position: absolute;
    right: 20rpx;
    bottom: 12rpx;
    font-size: 22rpx;
    color: $text-placeholder;
  }
}

.images {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 20rpx;

  &__item {
    position: relative;
    width: 150rpx;
    height: 150rpx;
  }

  &__pic {
    width: 150rpx;
    height: 150rpx;
    border-radius: 12rpx;
    background-color: #f2f2f2;
  }

  &__remove {
    position: absolute;
    top: -10rpx;
    right: -10rpx;
    width: 36rpx;
    height: 36rpx;
    line-height: 32rpx;
    text-align: center;
    font-size: 28rpx;
    color: #fff;
    background-color: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
  }

  &__add {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 150rpx;
    height: 150rpx;
    border: 1rpx dashed $border-color;
    border-radius: 12rpx;
    background-color: #fafafa;
  }

  &__plus {
    font-size: 44rpx;
    line-height: 44rpx;
    color: $text-placeholder;
  }

  &__hint {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $text-placeholder;
  }
}

.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);

  &__btn {
    height: 84rpx;
    line-height: 84rpx;
    text-align: center;
    border-radius: 42rpx;
    font-size: 30rpx;

    &--primary {
      color: #fff;
      background-color: $primary;
    }

    &--disabled {
      opacity: 0.6;
    }

    &--hover {
      opacity: 0.85;
    }
  }
}
</style>
