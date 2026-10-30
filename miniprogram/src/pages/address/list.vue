<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/empty-state.vue'
import RegionPicker from '@/components/region-picker.vue'
import { addressApi } from '@/api/user'
import type { Address, AddressForm } from '@/types/api'
import { useTheme } from '@/composables/useTheme'

const { theme, themePageStyle } = useTheme()

const list = ref<Address[]>([])
const selectMode = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
/** 省市区选择器 */
const regionPickerVisible = ref(false)
/** 编辑时用于定位初始选中项 */
const initialRegion = ref<{ province?: string, city?: string, district?: string }>({})

const form = reactive<AddressForm>({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0
})

async function load() {
  list.value = await addressApi.list()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    receiver: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: 0
  })
}

function openCreate() {
  resetForm()
  initialRegion.value = {}
  showForm.value = true
}

function openEdit(address: Address) {
  editingId.value = address.id
  Object.assign(form, {
    receiver: address.receiver,
    phone: address.phone,
    province: address.province,
    city: address.city,
    district: address.district,
    detail: address.detail,
    isDefault: address.isDefault
  })
  initialRegion.value = { province: address.province, city: address.city, district: address.district }
  showForm.value = true
}

/** 省市区选择完成：写入标准名称（与 region 表一致，运费按名称精确匹配） */
function onRegionConfirm(value: { province: string, city: string, district: string }) {
  form.province = value.province
  form.city = value.city
  form.district = value.district
}

async function submit() {
  if (!form.receiver.trim()) {
    uni.showToast({ title: '请填写收货人', icon: 'none' })
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return
  }
  if (!form.province.trim() || !form.city.trim() || !form.district.trim()) {
    uni.showToast({ title: '请选择省市区', icon: 'none' })
    return
  }
  if (!form.detail.trim()) {
    uni.showToast({ title: '请填写详细地址', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await addressApi.update(editingId.value, { ...form })
    } else {
      await addressApi.create({ ...form })
    }
    showForm.value = false
    await load()
    uni.showToast({ title: '保存成功', icon: 'success' })
  } finally {
    submitting.value = false
  }
}

async function remove(address: Address) {
  const res = await uni.showModal({ title: '提示', content: '确定删除该地址吗？' })
  if (!res.confirm) return
  await addressApi.remove(address.id)
  await load()
}

async function setDefault(address: Address) {
  if (address.isDefault === 1) return
  await addressApi.setDefault(address.id)
  await load()
}

function choose(address: Address) {
  if (!selectMode.value) return
  uni.$emit('address:select', address)
  uni.navigateBack()
}

onLoad((options) => {
  selectMode.value = options?.select === '1'
  load()
})
</script>

<template>
  <!-- 主题变量注入：后台启用主题后页面底色与全部语义色跟随变化 -->
  <!-- #ifdef MP-WEIXIN -->
  <page-meta :page-style="themePageStyle" />
  <!-- #endif -->
  <view class="page">
    <EmptyState v-if="!list.length" text="还没有收货地址" tip="点击下方按钮新增" />

    <view v-for="address in list" :key="address.id" class="address" @tap="choose(address)">
      <view class="address__body">
        <view class="address__line">
          <text class="address__receiver">{{ address.receiver }}</text>
          <text class="address__phone">{{ address.phone }}</text>
          <text v-if="address.isDefault === 1" class="address__tag">默认</text>
        </view>
        <text class="address__detail">
          {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detail }}
        </text>
      </view>
      <view class="address__actions">
        <text class="address__action" @tap.stop="setDefault(address)">设为默认</text>
        <text class="address__action" @tap.stop="openEdit(address)">编辑</text>
        <text class="address__action address__action--danger" @tap.stop="remove(address)">删除</text>
      </view>
    </view>

    <view class="footer safe-bottom">
      <view class="footer__btn" @tap="openCreate">+ 新增收货地址</view>
    </view>

    <view v-if="showForm" class="modal">
      <view class="modal__mask" @tap="showForm = false" />
      <view class="modal__panel">
        <text class="modal__title">{{ editingId ? '编辑地址' : '新增地址' }}</text>
        <view class="form">
          <view class="form__item">
            <text class="form__label">收货人</text>
            <input v-model="form.receiver" class="form__input" placeholder="请填写收货人姓名" />
          </view>
          <view class="form__item">
            <text class="form__label">手机号</text>
            <input v-model="form.phone" class="form__input" type="number" maxlength="11" placeholder="请填写手机号" />
          </view>
          <view class="form__item" @tap="regionPickerVisible = true">
            <text class="form__label">所在地区</text>
            <text v-if="form.province" class="form__value">
              {{ form.province }} {{ form.city }} {{ form.district }}
            </text>
            <text v-else class="form__placeholder">请选择省 / 市 / 区</text>
            <text class="form__arrow">›</text>
          </view>
          <view class="form__item">
            <text class="form__label">详细地址</text>
            <input v-model="form.detail" class="form__input" placeholder="街道、门牌号" />
          </view>
          <view class="form__item form__item--switch">
            <text class="form__label">设为默认</text>
            <switch
              :checked="form.isDefault === 1"
              :color="theme.primaryColor"
              @change="(e) => (form.isDefault = e.detail.value ? 1 : 0)"
            />
          </view>
        </view>
        <view class="modal__actions">
          <view class="modal__btn" @tap="showForm = false">取消</view>
          <view class="modal__btn modal__btn--primary" @tap="submit">
            {{ submitting ? '保存中…' : '保存' }}
          </view>
        </view>
      </view>
    </view>

    <RegionPicker
      v-model="regionPickerVisible"
      :initial="initialRegion"
      @confirm="onRegionConfirm"
    />
  </view>
</template>

<style scoped lang="scss">

.page {
  min-height: 100vh;
  background-color: $page-bg;
  padding: 20rpx 24rpx 160rpx;
}

.address {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 20rpx;

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

  &__tag {
    margin-left: 12rpx;
    padding: 2rpx 10rpx;
    font-size: 20rpx;
    color: $primary;
    background-color: $primary-light;
    border-radius: 6rpx;
  }

  &__detail {
    display: block;
    margin-top: 12rpx;
    font-size: 26rpx;
    color: $text-secondary;
    line-height: 36rpx;
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 28rpx;
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid $border-color;
  }

  &__action {
    font-size: 24rpx;
    color: $text-secondary;

    &--danger {
      color: $danger;
    }
  }
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx;
  background-color: #fff;

  &__btn {
    height: 84rpx;
    line-height: 84rpx;
    text-align: center;
    background-color: $primary;
    color: #fff;
    border-radius: 44rpx;
    font-size: 28rpx;
  }
}

.modal {
  position: fixed;
  inset: 0;
  z-index: 100;

  &__mask {
    position: absolute;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.45);
  }

  &__panel {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #fff;
    border-radius: 24rpx 24rpx 0 0;
    padding: 32rpx 24rpx calc(32rpx + env(safe-area-inset-bottom));
  }

  &__title {
    display: block;
    text-align: center;
    font-size: 32rpx;
    font-weight: 600;
    margin-bottom: 20rpx;
  }

  &__actions {
    display: flex;
    gap: 20rpx;
    margin-top: 28rpx;
  }

  &__btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    border-radius: 44rpx;
    font-size: 28rpx;
    background-color: #f5f5f5;
    color: $text-secondary;

    &--primary {
      background-color: $primary;
      color: #fff;
    }
  }
}

.form {
  &__item {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid $border-color;

    &--switch {
      border-bottom: none;
      justify-content: space-between;
    }
  }

  &__label {
    width: 160rpx;
    font-size: 28rpx;
    color: $text-main;
  }

  &__input {
    flex: 1;
    font-size: 28rpx;
  }

  &__value {
    flex: 1;
    font-size: 28rpx;
    color: $text-main;
  }

  &__placeholder {
    flex: 1;
    font-size: 28rpx;
    color: $text-placeholder;
  }

  &__arrow {
    font-size: 36rpx;
    color: $text-placeholder;
    margin-left: 12rpx;
  }
}
</style>
