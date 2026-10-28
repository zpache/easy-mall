<script lang="ts" setup>
import type { PageSchema } from '../widgets/types';

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { getDecoratePreview } from '#/api/decorate/page';

import { getWidget, normalizeSchema } from '../widgets';

const route = useRoute();
const code = computed(() => String(route.params.code || ''));
const loading = ref(true);
const error = ref('');
const pageName = ref('');
const schema = ref<PageSchema>({ components: [], page: { bgColor: '#f5f6f7', bgImage: '', padding: 0, title: '' } });

const visibleComponents = computed(() =>
  schema.value.components.filter(
    (item) => item.visible !== false && getWidget(item.type),
  ),
);

onMounted(async () => {
  try {
    const result = await getDecoratePreview(code.value);
    pageName.value = result.name;
    schema.value = normalizeSchema(JSON.parse(result.schemaJson), result.name);
  } catch (e: any) {
    error.value = e?.message || '预览加载失败';
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="preview-root">
    <div v-if="loading" class="state">正在加载页面…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else class="phone">
      <div
        class="screen"
        :style="{
          background: schema.page.bgImage
            ? `url(${schema.page.bgImage}) center / cover no-repeat`
            : schema.page.bgColor,
          padding: `${schema.page.padding ?? 0}px 0`,
        }"
      >
        <component
          :is="getWidget(item.type)?.component"
          v-for="item in visibleComponents"
          :key="item.id"
          :config="item.props"
        />
      </div>
    </div>
    <p v-if="!loading && !error" class="footer">易商城 · {{ pageName }}</p>
  </div>
</template>

<style scoped>
.preview-root {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 0;
  background: #f2f3f5;
}
.state {
  padding-top: 40vh;
  font-size: 14px;
  color: #909399;
}
.state.error {
  color: #f56c6c;
}
.phone {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  background: #fff;
  box-shadow: 0 0 24px rgb(0 0 0 / 8%);
}
.screen {
  min-height: 100vh;
}
.footer {
  padding: 12px 0 20px;
  font-size: 11px;
  color: #b1b3b8;
}
</style>
