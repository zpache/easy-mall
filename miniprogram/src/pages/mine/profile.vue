<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { uploadAvatar } from '@/api/file'
import { userApi } from '@/api/user'
import { useUserStore } from '@/store/user'
import { resolveMediaUrl } from '@/utils/url'

const userStore = useUserStore()

const genderLabels = ['保密', '男', '女']

const originAvatar = ref(userStore.profile?.avatar || '')
/** 新选的本地临时文件，保存时才真正上传 */
const pendingAvatar = ref('')
const nickname = ref(normalizeNickname(userStore.profile?.nickname))
const gender = ref(userStore.profile?.gender ?? 0)
const saving = ref(false)

const avatarPreview = computed(() =>
  pendingAvatar.value ? pendingAvatar.value : resolveMediaUrl(originAvatar.value)
)

function normalizeNickname(value?: string) {
  // 历史数据里自动注册的昵称都是「微信用户」，当作未填写处理
  return !value || value === '微信用户' ? '' : value
}

function syncFromProfile() {
  originAvatar.value = userStore.profile?.avatar || ''
  nickname.value = normalizeNickname(userStore.profile?.nickname)
  gender.value = userStore.profile?.gender ?? 0
}

function applyLocalAvatar(path: string) {
  pendingAvatar.value = path
}

function onChooseAvatar(e: any) {
  const url = e?.detail?.avatarUrl
  if (url) applyLocalAvatar(url)
}

/** H5 等非小程序端没有 chooseAvatar，退化为相册选图 */
function chooseFromAlbum() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => {
      const path = res.tempFilePaths?.[0]
      if (path) applyLocalAvatar(path)
    }
  })
}

function onNicknameBlur(e: any) {
  const value = e?.detail?.value
  if (typeof value === 'string') nickname.value = value
}

function onGenderChange(e: any) {
  gender.value = Number(e.detail.value)
}

async function save() {
  if (saving.value) return
  const name = (nickname.value || '').trim()
  if (!name) {
    uni.showToast({ title: '请先填写昵称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    let avatar = originAvatar.value
    if (pendingAvatar.value) {
      avatar = await uploadAvatar(pendingAvatar.value)
    }
    const profile = await userApi.updateProfile({ nickname: name, avatar, gender: gender.value })
    userStore.setProfile(profile)
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 600)
  } catch (e) {
    // request / upload 层已提示，这里只兜底日志
    console.error('save profile failed', e)
  } finally {
    saving.value = false
  }
}

onLoad(async () => {
  if (!userStore.isLogin) {
    uni.redirectTo({ url: '/pages/login/index' })
    return
  }
  if (!userStore.profile) {
    try {
      await userStore.fetchProfile()
    } catch {
      userStore.logout()
      return
    }
  }
  // 直接进入本页时用最新资料回填表单
  syncFromProfile()
})
</script>

<template>
  <view class="page">
    <view class="card">
      <!-- #ifdef MP-WEIXIN -->
      <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
        <image v-if="avatarPreview" class="avatar" :src="avatarPreview" mode="aspectFill" />
        <view v-else class="avatar avatar--empty">＋</view>
        <text class="avatar-tip">点击选择头像</text>
      </button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <view class="avatar-btn" @tap="chooseFromAlbum">
        <image v-if="avatarPreview" class="avatar" :src="avatarPreview" mode="aspectFill" />
        <view v-else class="avatar avatar--empty">＋</view>
        <text class="avatar-tip">点击选择头像</text>
      </view>
      <!-- #endif -->

      <view class="cell">
        <text class="cell__label">昵称</text>
        <input
          v-model="nickname"
          class="cell__input"
          type="nickname"
          placeholder="点击填写昵称"
          placeholder-class="cell__placeholder"
          maxlength="20"
          @blur="onNicknameBlur"
        />
      </view>

      <picker mode="selector" :range="genderLabels" :value="gender" @change="onGenderChange">
        <view class="cell">
          <text class="cell__label">性别</text>
          <text class="cell__value">{{ genderLabels[gender] }} ›</text>
        </view>
      </picker>
    </view>

    <view class="tip">
      <text class="tip__title">为什么昵称是「微信用户」？</text>
      <text class="tip__text">
        微信自 2022 年 10 月 25 日起，登录凭证（jscode2session）只返回 openid，
        不再下发昵称和头像；wx.getUserProfile 也已被收回，只会返回「微信用户」和灰色头像。
        需要你在这里用微信提供的「头像昵称填写能力」主动授权一次，之后商城会记住。
      </text>
    </view>

    <button class="save-btn" :class="{ 'save-btn--disabled': saving }" @tap="save">
      {{ saving ? '保存中…' : '保存' }}
    </button>
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding: 24rpx 24rpx 60rpx;
}

.card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 0 24rpx;
}

.avatar-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0 24rpx;
  background-color: transparent;
  line-height: normal;

  &::after {
    border: none;
  }
}

.avatar {
  width: 144rpx;
  height: 144rpx;
  border-radius: 50%;
  background-color: $page-bg;

  &--empty {
    display: flex;
    align-items: center;
    justify-content: center;
    color: $text-placeholder;
    font-size: 56rpx;
    border: 2rpx dashed $border-color;
  }
}

.avatar-tip {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: $text-secondary;
}

.cell {
  display: flex;
  align-items: center;
  min-height: 104rpx;
  border-top: 1rpx solid $border-color;

  &__label {
    width: 140rpx;
    font-size: 28rpx;
    color: $text-main;
  }

  &__input {
    flex: 1;
    font-size: 28rpx;
    color: $text-main;
    text-align: right;
  }

  &__placeholder {
    color: $text-placeholder;
    font-size: 28rpx;
  }

  &__value {
    flex: 1;
    font-size: 28rpx;
    color: $text-secondary;
    text-align: right;
  }
}

.tip {
  margin-top: 24rpx;
  padding: 24rpx;
  background-color: $primary-light;
  border-radius: 16rpx;

  &__title {
    display: block;
    font-size: 26rpx;
    font-weight: 600;
    color: $primary;
  }

  &__text {
    display: block;
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 38rpx;
    color: $text-secondary;
  }
}

.save-btn {
  margin-top: 48rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background-color: $primary;
  color: #fff;
  border-radius: 46rpx;
  font-size: 30rpx;

  &::after {
    border: none;
  }

  &--disabled {
    background-color: #ffb899;
  }
}
</style>
