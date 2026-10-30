<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import DecorateFloatBall from '@/components/decorate/float-ball.vue'
import DecorateRender from '@/components/decorate/render.vue'
import DecorateTabbar from '@/components/decorate/tabbar.vue'
import { decorateApi } from '@/api/decorate'
import { useDecorateStore } from '@/store/decorate'
import { parseNav, parseSchema } from '@/utils/decorate'
import type { PageSchema } from '@/types/decorate'
import { useTheme } from '@/composables/useTheme'

const { themePageStyle } = useTheme()

/**
 * 通用装修页：TabBar 里「自定义页面」跳转的落地页。
 * 小程序页面必须在 pages.json 声明，所以用同一个页面承载所有微页面。
 */
const decorateStore = useDecorateStore()

const pageId = ref('')
const schema = ref<PageSchema>({
  page: { title: '', bgColor: '', bgImage: '', padding: 0 },
  components: []
})
const loading = ref(true)
const error = ref('')

/**
 * 页面背景：装修页单独设置过背景色（非默认值）则优先，
 * 否则跟随线上主题的背景色（换肤）。
 */
const DEFAULT_PAGE_BG = '#f6f7f9'
const pageBg = computed(() => {
  const pageColor = schema.value.page.bgColor
  if (pageColor && pageColor !== DEFAULT_PAGE_BG) return pageColor
  return decorateStore.theme.pageBgColor || DEFAULT_PAGE_BG
})

async function load() {
  if (!pageId.value) {
    error.value = '缺少页面参数'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  decorateStore.ensureTheme()
  try {
    const data = await decorateApi.page(pageId.value)
    schema.value = parseSchema(data.schemaJson, data.name)
    decorateStore.setNav(parseNav(data.nav))
    uni.setNavigationBarTitle({ title: data.name })
  } catch (e: any) {
    error.value = e?.message || '页面加载失败'
    await decorateStore.ensureNav()
  } finally {
    loading.value = false
  }
}

function toSearch() {
  uni.navigateTo({ url: '/pages/search/index' })
}

/** 标题组件「更多」：默认进入商品列表页，展示全品类商品 */
function toGoodsList() {
  uni.navigateTo({ url: '/pages/goods/list' })
}

onLoad((query) => {
  pageId.value = String(query?.id || '')
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="dpage" :style="{ background: pageBg }">
    <image
      v-if="schema.page.bgImage"
      class="dpage__bg"
      :src="schema.page.bgImage"
      mode="aspectFill"
    />
    <view class="dpage__body" :style="{ paddingBottom: `${schema.page.padding || 0}px` }">
      <view v-if="loading" class="dpage__state">正在加载…</view>
      <view v-else-if="!schema.components.length" class="dpage__state">
        {{ error || '页面还没有内容' }}
      </view>
      <DecorateRender v-else :schema="schema" @more="toGoodsList" @search="toSearch" />
    </view>

    <DecorateFloatBall :config="decorateStore.nav.floatBall" :bottom="150" />
    <DecorateTabbar
      :config="decorateStore.nav.tabbar"
      current="/pages/decorate/index"
      :page-id="pageId"
    />
  </view>
</template>

<style scoped lang="scss">
.dpage {
  position: relative;
  min-height: 100vh;
  padding-bottom: 130rpx;
}

.dpage__bg {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.dpage__body {
  position: relative;
}

.dpage__state {
  padding: 160rpx 40rpx;
  font-size: 26rpx;
  color: #999;
  text-align: center;
}
</style>
