<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)

async function login() {
  if (loading.value) return
  loading.value = true
  try {
    await userStore.login()
    uni.showToast({ title: '登录成功', icon: 'success' })
    // 微信只下发 openid，首次登录顺手引导补全头像昵称
    const needProfile = userStore.profileIncomplete
    setTimeout(() => {
      if (needProfile) {
        uni.redirectTo({ url: '/pages/mine/profile' })
        return
      }
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/index/index' })
      }
    }, 500)
  } catch (e) {
    // request 层已提示错误信息，这里只兜底日志
    console.error('login failed', e)
  } finally {
    loading.value = false
  }
}

function skip() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="page">
    <view class="logo">🛍</view>
    <text class="title">欢迎来到 easy-mall</text>
    <text class="desc">登录后可同步购物车与订单</text>

    <view class="login-btn" :class="{ 'login-btn--loading': loading }" @tap="login">
      {{ loading ? '登录中…' : '微信一键登录' }}
    </view>

    <text class="skip" @tap="skip">暂不登录，先逛逛</text>

    <text class="tip">开发环境提示：后端 easy-mall.wx.mock=true 时，任意 code 均可登录</text>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 48rpx 0;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #ff8a4c 0%, $primary 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72rpx;
}

.title {
  margin-top: 40rpx;
  font-size: 36rpx;
  font-weight: 600;
}

.desc {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: $text-secondary;
}

.login-btn {
  width: 100%;
  margin-top: 80rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background-color: $primary;
  color: #fff;
  border-radius: 46rpx;
  font-size: 30rpx;

  &--loading {
    background-color: #ffb899;
  }
}

.skip {
  margin-top: 32rpx;
  font-size: 26rpx;
  color: $text-secondary;
}

.tip {
  margin-top: auto;
  margin-bottom: 60rpx;
  font-size: 22rpx;
  color: $text-placeholder;
  text-align: center;
  line-height: 34rpx;
}
</style>
