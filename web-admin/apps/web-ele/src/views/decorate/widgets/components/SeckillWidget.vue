<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{ config: Record<string, any> }>();

const remain = ref({ hours: '00', minutes: '00', seconds: '00' });
let timer: any;

function tick() {
  const end = props.config.endTime ? new Date(String(props.config.endTime).replace(/-/g, '/')).getTime() : 0;
  const diff = Math.max(0, end - Date.now());
  const hours = Math.floor(diff / 3_600_000);
  const minutes = Math.floor((diff % 3_600_000) / 60_000);
  const seconds = Math.floor((diff % 60_000) / 1000);
  remain.value = {
    hours: String(hours).padStart(2, '0'),
    minutes: String(minutes).padStart(2, '0'),
    seconds: String(seconds).padStart(2, '0'),
  };
}

onMounted(() => {
  tick();
  timer = setInterval(tick, 1000);
});
onBeforeUnmount(() => clearInterval(timer));

const price = computed(() => Number(props.config.price ?? 0).toFixed(2));
const originPrice = computed(() => Number(props.config.originPrice ?? 0).toFixed(2));
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
        <span>{{ remain.hours }}</span><i>:</i><span>{{ remain.minutes }}</span><i>:</i><span>{{ remain.seconds }}</span>
      </div>
    </div>
    <div class="goods">
      <img v-if="config.image" class="cover" :src="config.image" alt="seckill" />
      <div v-else class="cover placeholder" />
      <div class="info">
        <p class="name">{{ config.goodsName || '秒杀商品' }}</p>
        <div class="price-row">
          <span class="price"><i>¥</i>{{ price }}</span>
          <span class="origin">¥{{ originPrice }}</span>
        </div>
        <span class="btn">立即抢购</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.widget-seckill {
  padding: 12px;
}
.header {
  display: flex;
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
  gap: 2px;
  align-items: center;
  font-size: 10px;
  color: #fff;
}
.countdown span {
  padding: 2px 4px;
  background: #333;
  border-radius: 3px;
}
.goods {
  display: flex;
  gap: 10px;
  padding: 8px;
  background: #fff;
  border-radius: 8px;
}
.cover {
  width: 96px;
  height: 96px;
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
}
.name {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  font-size: 13px;
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
  font-size: 18px;
  font-weight: 700;
  color: #e02020;
}
.price i {
  font-size: 11px;
  font-style: normal;
}
.origin {
  font-size: 11px;
  color: #999;
  text-decoration: line-through;
}
.btn {
  align-self: flex-start;
  padding: 4px 12px;
  margin-top: 6px;
  font-size: 11px;
  color: #fff;
  background: linear-gradient(90deg, #ff6b00, #e02020);
  border-radius: 12px;
}
</style>
