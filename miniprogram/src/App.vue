<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'

onLaunch(() => {
  const userStore = useUserStore()
  // 已有 token 时静默刷新用户信息与购物车角标
  if (userStore.isLogin) {
    userStore.fetchProfile().catch(() => userStore.logout())
    useCartStore().refreshCount().catch(() => undefined)
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
</style>
