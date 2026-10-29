<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { setUnauthorizedHandler } from '@/api/request'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'

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
})

onShow(() => {
  // 预留：小程序从后台切回前台时的刷新逻辑
})
</script>

<style lang="scss">
@import '@/styles/common.scss';

page {
  background-color: $page-bg;
  color: #1f1f1f;
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
