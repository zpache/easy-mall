<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import DecorateFloatBall from '@/components/decorate/float-ball.vue'
import DecorateNavbar from '@/components/decorate/navbar.vue'
import DecorateRender from '@/components/decorate/render.vue'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import { decorateApi } from '@/api/decorate'
import { useDecorateStore } from '@/store/decorate'
import { parseNav, parseSchema, px, statusBarHeight } from '@/utils/decorate'
import type { PageSchema } from '@/types/decorate'

const decorateStore = useDecorateStore()

const schema = ref<PageSchema>({
  page: { title: '商城首页', bgColor: '#f5f6f7', bgImage: '', padding: 0 },
  components: []
})
const loading = ref(true)
const error = ref('')

const navbarHeight = statusBarHeight() + 44
const immersive = computed(() => decorateStore.nav.navbar.style === 'immersive')

/** 常规样式下内容让开标题栏；沉浸式则让内容从屏幕顶端开始 */
const bodyPaddingTop = computed(() => (immersive.value ? '0px' : `${navbarHeight}px`))

/**
 * 页面背景：装修页单独设置过背景色（非默认值）则优先，
 * 否则跟随线上主题的背景色（换肤）。
 */
const DEFAULT_PAGE_BG = '#f5f6f7'
const pageBg = computed(() => {
  const pageColor = schema.value.page.bgColor
  if (pageColor && pageColor !== DEFAULT_PAGE_BG) return pageColor
  return decorateStore.theme.pageBgColor || DEFAULT_PAGE_BG
})

async function load() {
  loading.value = true
  error.value = ''
  decorateStore.ensureTheme()
  try {
    const data = await decorateApi.home()
    schema.value = parseSchema(data.schemaJson, data.name)
    decorateStore.setNav(parseNav(data.nav))
  } catch (e: any) {
    error.value = e?.message || '首页加载失败'
    await decorateStore.ensureNav()
  } finally {
    loading.value = false
  }
}

function toSearch() {
  uni.navigateTo({ url: '/pages/search/index' })
}

onLoad(() => {
  load()
})

onShow(() => {
  // 从其它 tab 切回时不重复请求，仅补齐导航配置
  decorateStore.ensureNav()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="home" :style="{ background: pageBg }">
    <DecorateNavbar :config="decorateStore.nav.navbar" />

    <!-- 页面背景图用 image 承载，小程序不支持 CSS 网络背景图 -->
    <image
      v-if="schema.page.bgImage"
      class="home__bg"
      :src="schema.page.bgImage"
      mode="aspectFill"
    />

    <view
      class="home__body"
      :style="{ paddingTop: bodyPaddingTop, paddingBottom: px(schema.page.padding, 0) }"
    >
      <view v-if="loading" class="home__state">正在加载首页…</view>
      <view v-else-if="!schema.components.length" class="home__state">
        {{ error || '首页还没有内容，请到后台「商城装修」中搭建' }}
      </view>
      <DecorateRender v-else :schema="schema" @search="toSearch" />
    </view>

    <DecorateFloatBall :config="decorateStore.nav.floatBall" :bottom="150" />
    <DecorateTabbar :config="decorateStore.nav.tabbar" current="/pages/index/index" />
  </view>
</template>

<style scoped lang="scss">
.home {
  position: relative;
  min-height: 100vh;
  padding-bottom: 130rpx;
}

.home__bg {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.home__body {
  position: relative;
}

.home__state {
  padding: 160rpx 40rpx;
  font-size: 26rpx;
  color: #999;
  text-align: center;
}
</style>
