<script lang="ts" setup>
defineProps<{ config: Record<string, any> }>();
</script>

<template>
  <div class="widget-shop" :style="{ background: config.bgColor || '#ffffff' }">
    <img v-if="config.logo" class="logo" :src="config.logo" alt="logo" />
    <div class="info">
      <p class="name">{{ config.name || '店铺名称' }}</p>
      <p class="desc">{{ config.desc }}</p>
    </div>
    <span class="phone">联系客服</span>
  </div>
</template>

<style scoped>
.widget-shop {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px;
}
.logo {
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 10px;
}
.info {
  flex: 1;
  min-width: 0;
}
.name {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.desc {
  margin: 3px 0 0;
  font-size: 11px;
  color: #999;
}
.phone {
  flex: none;
  padding: 5px 10px;
  font-size: 11px;
  color: #ff6b00;
  border: 1px solid #ffd9bf;
  border-radius: 12px;
}
</style>
