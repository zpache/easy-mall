<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import { getCouponList } from '#/api/marketing/coupon';

const props = defineProps<{ config: Record<string, any> }>();

/** 券字典模块级缓存：同一画布上多个优惠券组件共用一次请求 */
let poolPromise: null | Promise<any[]> = null;

const pool = ref<any[]>([]);

const ids = computed<number[]>(() =>
  Array.isArray(props.config.couponIds) ? props.config.couponIds : [],
);

/** 按配置顺序取券，已被删除/停用的券自动跳过 */
const coupons = computed(() => {
  if (ids.value.length === 0) {
    return [];
  }
  const map = new Map(pool.value.map((coupon) => [coupon.id, coupon]));
  return ids.value.map((id) => map.get(id)).filter(Boolean);
});

function cond(coupon: any) {
  return Number(coupon.threshold) > 0
    ? `满${coupon.threshold}可用`
    : '无门槛';
}

function valid(coupon: any) {
  if (coupon.validType === 2) {
    return `领取后${coupon.validDays || 7}天有效`;
  }
  return coupon.validEnd ? `有效期至 ${coupon.validEnd}` : '长期有效';
}

onMounted(async () => {
  if (!poolPromise) {
    poolPromise = getCouponList({ pageNum: 1, pageSize: 200 })
      .then((res) => res.items || [])
      .catch(() => {
        poolPromise = null;
        return [];
      });
  }
  pool.value = await poolPromise;
});
</script>

<template>
  <div class="widget-coupon" :style="{ background: config.bgColor || '#ffffff' }">
    <div class="header">
      <span class="title">{{ config.title || '领券中心' }}</span>
      <span class="more">更多 ›</span>
    </div>

    <div v-if="coupons.length > 0" class="list">
      <div v-for="item in coupons" :key="item.id" class="coupon">
        <div class="left">
          <span class="amount">
            <template v-if="item.type === 2">
              {{ Number(item.discount ?? 0) / 10 }}<i>折</i>
            </template>
            <template v-else><i>¥</i>{{ item.amount }}</template>
          </span>
          <span class="cond">{{ cond(item) }}</span>
        </div>
        <div class="right">
          <p class="name">{{ item.name }}</p>
          <p class="valid">{{ valid(item) }}</p>
        </div>
        <span class="btn">领取</span>
      </div>
    </div>

    <div v-else class="empty">
      <span class="empty__icon">🎫</span>
      <span class="empty__text">
        {{ ids.length > 0 ? '所选优惠券已下架' : '请在右侧属性面板选择优惠券' }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.widget-coupon {
  padding: 12px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.more {
  font-size: 11px;
  color: #999;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.coupon {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: linear-gradient(90deg, #fff5f5 0%, #ffffff 60%);
  border: 1px solid #ffe0e0;
  border-radius: 8px;
}
.left {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 62px;
  padding-right: 8px;
  border-right: 1px dashed #ffd0d0;
}
.amount {
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  color: #e02020;
}
.amount i {
  font-size: 11px;
  font-style: normal;
}
.cond {
  margin-top: 4px;
  font-size: 10px;
  color: #e02020;
}
.right {
  flex: 1;
  min-width: 0;
}
.name {
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  color: #333;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.valid {
  margin: 4px 0 0;
  font-size: 10px;
  color: #999;
}
.btn {
  flex: none;
  padding: 4px 10px;
  font-size: 11px;
  color: #fff;
  background: #e02020;
  border-radius: 12px;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 18px 0;
  background: #fafafa;
  border: 1px dashed #e4e7ed;
  border-radius: 8px;
}
.empty__icon {
  font-size: 18px;
  opacity: 0.5;
}
.empty__text {
  margin-top: 4px;
  font-size: 11px;
  color: #b1b3b8;
}
</style>
