<script setup lang="ts">
// #ifdef H5
import { watch } from 'vue'
// #endif
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { setUnauthorizedHandler } from '@/api/request'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { useDecorateStore } from '@/store/decorate'
import { THEME_VAR_MAP } from '@/composables/useTheme'

const decorateStore = useDecorateStore()

onLaunch(() => {
  const userStore = useUserStore()
  // 自定义登录态失效（过期/被踢）时，请求层自动静默重登后重试
  setUnauthorizedHandler(() => userStore.silentRelogin())
  // 已有 token 时静默刷新用户信息与购物车角标
  if (userStore.isLogin) {
    userStore.fetchProfile().catch(() => userStore.logout())
    useCartStore().refreshCount().catch(() => undefined)
  } else {
    // #ifdef MP-WEIXIN
    // 微信官方推荐的登录模式：打开小程序即静默 wx.login 换 code，
    // 由后端换取自定义登录态，全程无感
    userStore.login().catch(() => undefined)
    // #endif
  }
  // 线上主题：启动即拉一次，后台启用新主题后重开小程序即变色
  void decorateStore.ensureTheme(true)
})

onShow(() => {
  // 从后台切回前台：按 TTL 校验线上主题，管理端启用新主题后切回来就能同步
  void decorateStore.ensureTheme()
})

// #ifdef H5
// H5 没有 page-meta，主题变量直接写到根节点，等价于小程序端的页面级注入
watch(
  () => decorateStore.theme,
  (theme) => {
    const root = document.documentElement
    for (const [name, key] of THEME_VAR_MAP) root.style.setProperty(name, theme[key])
  },
  { immediate: true, deep: true }
)
// #endif
</script>

<style lang="scss">
@import '@/styles/common.scss';

page {
  background-color: $page-bg;
  color: $text-main;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, sans-serif;
}

/* #ifdef H5 */
/* 底部导航按「导航装修」配置由页面内组件渲染，隐藏 H5 内置 tabBar */
uni-tabbar {
  display: none !important;
}
/* #endif */
</style>
