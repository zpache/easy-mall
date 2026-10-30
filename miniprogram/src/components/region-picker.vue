<script setup lang="ts">
import { ref, watch } from 'vue'
import { regionApi } from '@/api/region'
import type { RegionOption } from '@/types/api'

/**
 * 省市区三级选择器（数据来自后端 region 表，逐级懒加载）。
 * 选出的名称与 region 表完全一致，运费规则按名称精确匹配。
 */
const props = defineProps<{
  modelValue: boolean
  /** 打开时用于定位初始选中项（名称匹配） */
  initial?: { province?: string, city?: string, district?: string }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', value: { province: string, city: string, district: string }): void
}>()

const provinces = ref<RegionOption[]>([])
const cities = ref<RegionOption[]>([])
const districts = ref<RegionOption[]>([])
/** picker-view 三列选中索引 */
const value = ref([0, 0, 0])
const loading = ref(false)

/** 子区域缓存：parentCode -> children，避免重复请求 */
const cache = new Map<string, RegionOption[]>()

async function loadChildren(parentCode: string): Promise<RegionOption[]> {
  if (!cache.has(parentCode)) {
    cache.set(parentCode, await regionApi.children(parentCode))
  }
  return cache.get(parentCode) ?? []
}

async function loadProvinces() {
  if (provinces.value.length) return
  loading.value = true
  try {
    provinces.value = await loadChildren('0')
  } finally {
    loading.value = false
  }
}

async function selectProvince(index: number) {
  const province = provinces.value[index]
  if (!province) {
    cities.value = []
    districts.value = []
    return
  }
  loading.value = true
  try {
    cities.value = await loadChildren(province.areaCode)
    districts.value = []
  } finally {
    loading.value = false
  }
}

async function selectCity(index: number) {
  const city = cities.value[index]
  if (!city) {
    districts.value = []
    return
  }
  loading.value = true
  try {
    districts.value = await loadChildren(city.areaCode)
  } finally {
    loading.value = false
  }
}

function onColumnChange(e: { detail: { column: number, value: number } }) {
  const { column, value: index } = e.detail
  if (column === 0) {
    value.value = [index, 0, 0]
    selectProvince(index)
  } else if (column === 1) {
    value.value = [value.value[0], index, 0]
    selectCity(index)
  } else if (column === 2) {
    value.value = [value.value[0], value.value[1], index]
  }
}

/** 打开时初始化列数据，并按 initial 名称定位选中项 */
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    loading.value = true
    try {
      await loadProvinces()
      let provinceIndex = 0
      let cityIndex = 0
      let districtIndex = 0

      if (props.initial?.province) {
        const found = provinces.value.findIndex((item) => item.name === props.initial?.province)
        if (found >= 0) provinceIndex = found
      }
      await selectProvince(provinceIndex)

      if (props.initial?.city) {
        const found = cities.value.findIndex((item) => item.name === props.initial?.city)
        if (found >= 0) cityIndex = found
      }
      await selectCity(cityIndex)

      if (props.initial?.district) {
        const found = districts.value.findIndex((item) => item.name === props.initial?.district)
        if (found >= 0) districtIndex = found
      }
      value.value = [provinceIndex, cityIndex, districtIndex]
    } finally {
      loading.value = false
    }
  }
)

function onConfirm() {
  const province = provinces.value[value.value[0]]
  const city = cities.value[value.value[1]]
  const district = districts.value[value.value[2]]
  if (!province || !city || !district) {
    uni.showToast({ title: '请选择完整的省市区', icon: 'none' })
    return
  }
  emit('confirm', { province: province.name, city: city.name, district: district.name })
  emit('update:modelValue', false)
}

function onCancel() {
  emit('update:modelValue', false)
}
</script>

<template>
  <view v-if="modelValue" class="picker">
    <view class="picker__mask" @tap="onCancel" />
    <view class="picker__panel">
      <view class="picker__head">
        <text class="picker__cancel" @tap="onCancel">取消</text>
        <text class="picker__title">所在地区</text>
        <text class="picker__ok" @tap="onConfirm">确定</text>
      </view>
      <view v-if="loading" class="picker__loading">加载中…</view>
      <picker-view
        v-else
        class="picker__view"
        :value="value"
        @change="onColumnChange"
      >
        <picker-view-column>
          <view v-for="item in provinces" :key="item.areaCode" class="picker__item">
            {{ item.name }}
          </view>
        </picker-view-column>
        <picker-view-column>
          <view v-for="item in cities" :key="item.areaCode" class="picker__item">
            {{ item.name }}
          </view>
        </picker-view-column>
        <picker-view-column>
          <view v-for="item in districts" :key="item.areaCode" class="picker__item">
            {{ item.name }}
          </view>
        </picker-view-column>
      </picker-view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.picker {
  position: fixed;
  inset: 0;
  z-index: 120;

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
    padding-bottom: env(safe-area-inset-bottom);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid $border-color;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 600;
  }

  &__cancel {
    font-size: 28rpx;
    color: $text-secondary;
  }

  &__ok {
    font-size: 28rpx;
    color: $primary;
    font-weight: 600;
  }

  &__loading {
    height: 480rpx;
    line-height: 480rpx;
    text-align: center;
    font-size: 26rpx;
    color: $text-placeholder;
  }

  &__view {
    height: 480rpx;
  }

  &__item {
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    font-size: 28rpx;
    color: $text-main;
  }
}
</style>
