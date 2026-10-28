<script lang="ts" setup>
import { computed } from 'vue';

const props = defineProps<{ config: Record<string, any> }>();

const coupons = computed(() =>
  Array.isArray(props.config.coupons) ? props.config.coupons : [],
);
</script>

<template>
  <div class="widget-coupon" :style="{ background: config.bgColor || '#ffffff' }">
    <div class="header">
      <span class="title">{{ config.title || '领券中心' }}</span>
      <span class="more">更多 ›</span>
    </div>
    <div class="list">
      <div v-for="(item, index) in coupons" :key="index" class="coupon">
        <div class="left">
          <span v-if="item.discount" class="amount">{{ (item.discount / 10).toFixed(1) }}<i>折</i></span>
          <span v-else class="amount"><i>¥</i>{{ item.amount }}</span>
          <span class="cond">
            {{ Number(item.threshold) > 0 ? `满${item.threshold}可用` : '无门槛' }}
          </span>
        </div>
        <div class="right">
          <p class="name">{{ item.name }}</p>
          <p class="valid">{{ item.validDays }} 天内有效</p>
        </div>
        <span class="btn">领取</span>
      </div>
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
</style>
