<script lang="ts" setup>
import { computed } from 'vue';

const props = defineProps<{ config: Record<string, any> }>();

const goods = computed(() => (Array.isArray(props.config.goods) ? props.config.goods : []));
const isGrid = computed(() => props.config.layout !== 'list');
const columns = computed(() => props.config.columns ?? 2);
</script>

<template>
  <div class="widget-goods" :style="{ background: config.bgColor || 'transparent' }">
    <div
      v-if="isGrid"
      class="grid"
      :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }"
    >
      <div v-for="item in goods" :key="item.id" class="card">
        <img class="cover" :src="item.image" :alt="item.name" />
        <div class="info">
          <p class="name">{{ item.name }}</p>
          <div class="bottom">
            <span class="price"><i>¥</i>{{ item.price }}</span>
            <span v-if="config.showSales" class="sales">已售 {{ item.sales }}</span>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="list">
      <div v-for="item in goods" :key="item.id" class="row">
        <img class="thumb" :src="item.image" :alt="item.name" />
        <div class="info">
          <p class="name">{{ item.name }}</p>
          <div class="bottom">
            <span class="price"><i>¥</i>{{ item.price }}</span>
            <span v-if="config.showSales" class="sales">已售 {{ item.sales }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.widget-goods {
  padding: 8px 10px;
}
.grid {
  display: grid;
  gap: 8px;
}
.card {
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
}
.cover {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}
.info {
  padding: 7px 8px 8px;
}
.name {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.35;
  color: #333;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 6px;
}
.price {
  font-size: 15px;
  font-weight: 600;
  color: #e02020;
}
.price i {
  font-size: 10px;
  font-style: normal;
}
.sales {
  font-size: 10px;
  color: #999;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.row {
  display: flex;
  gap: 10px;
  padding: 8px;
  background: #fff;
  border-radius: 8px;
}
.thumb {
  width: 84px;
  height: 84px;
  object-fit: cover;
  border-radius: 6px;
}
.row .info {
  flex: 1;
  padding: 0;
}
.row .name {
  font-size: 13px;
}
</style>
