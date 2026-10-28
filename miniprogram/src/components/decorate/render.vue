<script setup lang="ts">
import { computed } from 'vue'
import type { PageSchema } from '@/types/decorate'
import { openLink, px } from '@/utils/decorate'
import BannerWidget from './widgets/banner.vue'
import CouponWidget from './widgets/coupon.vue'
import GoodsWidget from './widgets/goods.vue'
import NavGridWidget from './widgets/nav-grid.vue'
import SeckillWidget from './widgets/seckill.vue'
import ShopInfoWidget from './widgets/shop-info.vue'

const props = defineProps<{ schema: PageSchema }>()
const emit = defineEmits<{ (e: 'search'): void }>()

const components = computed(() => props.schema.components)
</script>

<template>
  <view class="d-render">
    <template v-for="item in components" :key="item.id">
      <!-- 搜索框 -->
      <view v-if="item.type === 'search'" class="d-search" :style="{ padding: `${px(item.props.margin, 10)} 24rpx` }">
        <view
          class="d-search__bar"
          :style="{
            background: item.props.bgColor || '#ffffff',
            borderRadius: px(item.props.radius, 18),
            justifyContent: item.props.align === 'center' ? 'center' : 'flex-start'
          }"
          @tap="emit('search')"
        >
          <text class="d-search__icon">🔍</text>
          <text class="d-search__ph">{{ item.props.placeholder || '搜索商品' }}</text>
        </view>
      </view>

      <!-- 公告栏 -->
      <view
        v-else-if="item.type === 'notice'"
        class="d-notice"
        :style="{ background: item.props.bgColor || '#fff7e8', color: item.props.color || '#ff6b00' }"
      >
        <text class="d-notice__icon">📣</text>
        <text class="d-notice__text">{{ item.props.text || '公告内容' }}</text>
      </view>

      <!-- 辅助空白 -->
      <view
        v-else-if="item.type === 'blank'"
        :style="{ height: px(item.props.height, 12), background: item.props.bgColor || 'transparent' }"
      />

      <!-- 轮播图 -->
      <BannerWidget v-else-if="item.type === 'banner'" :config="item.props" />

      <!-- 单图广告 -->
      <view v-else-if="item.type === 'image'" class="d-image" :style="{ padding: `${px(item.props.margin, 12)} 24rpx` }">
        <image
          v-if="item.props.url"
          class="d-image__img"
          :src="item.props.url"
          mode="widthFix"
          :style="{ borderRadius: px(item.props.radius, 8), height: item.props.height ? px(item.props.height) : 'auto' }"
          @tap="openLink(item.props.link)"
        />
        <view
          v-else
          class="d-image__placeholder"
          :style="{ borderRadius: px(item.props.radius, 8), height: px(item.props.height, 120) }"
        >
          单图广告
        </view>
      </view>

      <!-- 图标导航 -->
      <NavGridWidget v-else-if="item.type === 'nav'" :config="item.props" />

      <!-- 标题 -->
      <view
        v-else-if="item.type === 'title'"
        class="d-title"
        :style="{
          padding: `${px(item.props.margin, 12)} 24rpx`,
          textAlign: item.props.align || 'left',
          background: item.props.bgColor || 'transparent'
        }"
      >
        <view class="d-title__row" :style="{ justifyContent: item.props.align === 'center' ? 'center' : 'space-between' }">
          <view class="d-title__texts">
            <text
              class="d-title__text"
              :style="{
                fontSize: px(item.props.size, 16),
                color: item.props.color || '#333333',
                fontWeight: item.props.bold ? 600 : 400
              }"
            >{{ item.props.text || '标题' }}</text>
            <text v-if="item.props.subtitle" class="d-title__sub">{{ item.props.subtitle }}</text>
          </view>
          <text v-if="item.props.more && item.props.align !== 'center'" class="d-title__more">更多 ›</text>
        </view>
      </view>

      <!-- 富文本 -->
      <view
        v-else-if="item.type === 'richText'"
        class="d-richtext"
        :style="{
          padding: `${px(item.props.margin, 12)} 24rpx`,
          color: item.props.color || '#333333',
          fontSize: px(item.props.size, 13)
        }"
      >
        <rich-text :nodes="item.props.content || '<p>在此填写富文本内容</p>'" />
      </view>

      <!-- 商品列表 -->
      <GoodsWidget v-else-if="item.type === 'goods'" :config="item.props" />

      <!-- 限时秒杀 -->
      <SeckillWidget v-else-if="item.type === 'seckill'" :config="item.props" />

      <!-- 优惠券 -->
      <CouponWidget v-else-if="item.type === 'coupon'" :config="item.props" />

      <!-- 店铺信息 -->
      <ShopInfoWidget v-else-if="item.type === 'shopInfo'" :config="item.props" />
    </template>
  </view>
</template>

<style scoped lang="scss">
.d-search {
  box-sizing: border-box;
}

.d-search__bar {
  display: flex;
  gap: 12rpx;
  align-items: center;
  height: 68rpx;
  padding: 0 24rpx;
}

.d-search__icon {
  font-size: 26rpx;
  color: #999;
}

.d-search__ph {
  overflow: hidden;
  font-size: 24rpx;
  color: #999;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.d-notice {
  display: flex;
  gap: 12rpx;
  align-items: center;
  height: 60rpx;
  padding: 0 24rpx;
  overflow: hidden;
  font-size: 24rpx;
}

.d-notice__icon {
  flex: none;
  font-size: 24rpx;
}

.d-notice__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.d-image {
  box-sizing: border-box;
}

.d-image__img {
  display: block;
  width: 100%;
}

.d-image__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #bbb;
  background-color: #eef0f2;
}

.d-title {
  box-sizing: border-box;
}

.d-title__row {
  display: flex;
  align-items: center;
}

.d-title__texts {
  display: flex;
  gap: 12rpx;
  align-items: baseline;
  min-width: 0;
}

.d-title__text {
  line-height: 1.3;
}

.d-title__sub {
  font-size: 22rpx;
  color: #999;
}

.d-title__more {
  flex: none;
  font-size: 22rpx;
  color: #999;
}

.d-richtext {
  box-sizing: border-box;
  line-height: 1.7;
  overflow-wrap: break-word;
}
</style>
