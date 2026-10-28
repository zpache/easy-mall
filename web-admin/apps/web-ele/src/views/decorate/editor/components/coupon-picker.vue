<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import { ElOption, ElSelect } from 'element-plus';

import { getCouponList } from '#/api/marketing/coupon';

defineOptions({ name: 'CouponPicker' });

const props = defineProps<{
  max?: number;
  modelValue: any;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: any];
}>();

interface CouponOption {
  label: string;
  value: string;
}

const options = ref<CouponOption[]>([]);
const loading = ref(false);
const failed = ref(false);

/** 存的是券的随机码 couponCode，不用自增主键 */
const value = computed<string[]>({
  get: () =>
    Array.isArray(props.modelValue)
      ? props.modelValue.map(String)
      : [],
  set: (val) => emit('update:modelValue', val),
});

const limit = computed(() => props.max ?? 6);

/** 与小程序端券卡展示口径一致：满减/无门槛看面额，折扣券看折扣 */
function format(coupon: any) {
  const face =
    coupon.type === 2
      ? `${Number(coupon.discount ?? 0) / 10}折`
      : `¥${Number(coupon.amount ?? 0)}`;
  const cond =
    Number(coupon.threshold) > 0 ? `满${coupon.threshold}` : '无门槛';
  return `${coupon.name}（${face} ${cond}）`;
}

onMounted(async () => {
  loading.value = true;
  try {
    const { items } = await getCouponList({
      pageNum: 1,
      pageSize: 200,
      status: 1,
    });
    options.value = (items || [])
      .filter((coupon: any) => coupon.couponCode)
      .map((coupon: any) => ({
        label: format(coupon),
        value: coupon.couponCode,
      }));
  } catch {
    failed.value = true;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="coupon-picker">
    <ElSelect
      v-model="value"
      class="w-full"
      collapse-tags
      collapse-tags-tooltip
      filterable
      :loading="loading"
      :max-collapse-tags="1"
      :multiple-limit="limit"
      multiple
      placeholder="选择优惠券"
      size="small"
    >
      <ElOption
        v-for="option in options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </ElSelect>
    <p v-if="failed" class="hint hint--error">
      券列表加载失败，请检查营销中心是否有可用券
    </p>
    <p v-else-if="!loading && options.length === 0" class="hint">
      营销中心暂无可用券，请先创建并启用优惠券
    </p>
    <p v-else-if="value.length === 0" class="hint">
      未选择优惠券时小程序端不展示该模块的券卡
    </p>
  </div>
</template>

<style scoped>
.coupon-picker {
  width: 100%;
}
.hint {
  margin: 4px 0 0;
  font-size: 10px;
  line-height: 1.5;
  color: #a8abb2;
}
.hint--error {
  color: #f56c6c;
}
</style>
