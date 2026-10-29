<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{ config: Record<string, any> }>();

/** 剩余时间：超过 24 小时自动进位到「天」 */
const remain = ref({ days: 0, hours: 0, minutes: 0, seconds: 0 });
let timer: any;

const pad2 = (n: number) => String(n).padStart(2, '0');

function tick() {
  const end = props.config.endTime ? new Date(String(props.config.endTime).replaceAll('-', '/')).getTime() : 0;
  const diff = Math.max(0, end - Date.now());
  remain.value = {
    days: Math.floor(diff / 86_400_000),
    hours: Math.floor((diff % 86_400_000) / 3_600_000),
    minutes: Math.floor((diff % 3_600_000) / 60_000),
    seconds: Math.floor((diff % 60_000) / 1000),
  };
}

onMounted(() => {
  tick();
  timer = setInterval(tick, 1000);
});
onBeforeUnmount(() => clearInterval(timer));

/**
 * 秒杀商品列表：新版为 goods 选择器快照数组（含 seckillPrice）；
 * 旧版手填单商品字段做兼容展示。
 */
const goodsList = computed<any[]>(() => {
  if (Array.isArray(props.config.goods) && props.config.goods.length > 0) {
    return props.config.goods;
  }
  const { goodsName, image, price, originPrice } = props.config;
  if (goodsName || image) {
    return [
      {
        image: image || '',
        name: goodsName || '秒杀商品',
        price: Number(originPrice ?? 0),
        seckillPrice: Number(price ?? 0),
      },
    ];
  }
  return [];
});

function priceOf(item: any) {
  return Number(item.seckillPrice ?? item.price ?? 0).toFixed(2);
}

function originOf(item: any) {
  const origin = Number(item.price ?? 0);
  const seckill = Number(item.seckillPrice ?? 0);
  // 没填秒杀价或与原价相同就不显示划线原价
  return seckill > 0 && origin > seckill ? origin.toFixed(2) : '';
}

/**
 * 倒计时片段：
 * - 满 24 小时：20天 : 1小时 : 03分 : 04秒
 * - 不足 24 小时：1541:03:25 的纯数字形式（保持原有观感）
 */
const segments = computed(() => {
  const { days, hours, minutes, seconds } = remain.value;
  if (days > 0) {
    return [
      { num: String(days), unit: '天' },
      { num: String(hours), unit: '小时' },
      { num: pad2(minutes), unit: '分' },
      { num: pad2(seconds), unit: '秒' },
    ];
  }
  return [
    { num: pad2(hours), unit: '' },
    { num: pad2(minutes), unit: '' },
    { num: pad2(seconds), unit: '' },
  ];
});
</script>

<template>
  <div class="widget-seckill" :style="{ background: config.bgColor || '#fff1f0' }">
    <div class="header">
      <div class="left">
        <span class="flag">秒杀</span>
        <span class="title">{{ config.title || '限时秒杀' }}</span>
        <span class="subtitle">{{ config.subtitle }}</span>
      </div>
      <div class="countdown">
        <template v-for="(seg, i) in segments" :key="i">
          <i v-if="i > 0" class="colon">:</i>
          <span>{{ seg.num }}</span>
          <i v-if="seg.unit" class="unit">{{ seg.unit }}</i>
        </template>
      </div>
    </div>
    <div class="goods-scroll">
      <div v-for="(item, index) in goodsList" :key="item.id ?? index" class="goods">
        <img v-if="item.image" class="cover" :src="item.image" alt="seckill" />
        <div v-else class="cover placeholder"></div>
        <div class="info">
          <p class="name">{{ item.name || '秒杀商品' }}</p>
          <div class="price-row">
            <span class="price"><i>¥</i>{{ priceOf(item) }}</span>
            <span v-if="originOf(item)" class="origin">¥{{ originOf(item) }}</span>
          </div>
          <span class="btn">立即抢购</span>
        </div>
      </div>
      <div v-if="goodsList.length === 0" class="empty">请选择秒杀商品</div>
    </div>
  </div>
</template>

<style scoped>
.widget-seckill {
  padding: 12px;
}
.header {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.left {
  display: flex;
  gap: 6px;
  align-items: center;
}
.flag {
  padding: 1px 6px;
  font-size: 10px;
  color: #fff;
  background: #e02020;
  border-radius: 3px;
}
.title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.subtitle {
  font-size: 10px;
  color: #999;
}
.countdown {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
  font-size: 10px;
  color: #fff;
}
.countdown span {
  padding: 2px 4px;
  background: #333;
  border-radius: 3px;
}
.countdown .colon,
.countdown .unit {
  font-style: normal;
  color: #333;
}
.goods-scroll {
  display: flex;
  gap: 8px;
  padding: 2px;
  overflow-x: auto;
}
.goods {
  display: flex;
  flex: none;
  gap: 8px;
  width: 240px;
  padding: 8px;
  background: #fff;
  border-radius: 8px;
}
.cover {
  flex: none;
  width: 76px;
  height: 76px;
  object-fit: cover;
  border-radius: 6px;
}
.placeholder {
  background: #eef0f2;
}
.info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}
.name {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  color: #333;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.price-row {
  display: flex;
  gap: 6px;
  align-items: baseline;
  margin-top: auto;
}
.price {
  font-size: 16px;
  font-weight: 700;
  color: #e02020;
}
.price i {
  font-size: 10px;
  font-style: normal;
}
.origin {
  font-size: 10px;
  color: #999;
  text-decoration: line-through;
}
.btn {
  align-self: flex-start;
  padding: 2px 10px;
  margin-top: 4px;
  font-size: 10px;
  color: #fff;
  background: linear-gradient(90deg, #ff6b00, #e02020);
  border-radius: 10px;
}
.empty {
  padding: 16px 0;
  font-size: 12px;
  color: #bbb;
  text-align: center;
}
</style>
