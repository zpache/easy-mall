<script lang="ts" setup>
import type { NavConfig } from '../nav/config';
import type { PageSchema } from '../widgets/types';

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { getPreviewDecorateNav } from '#/api/decorate/nav';
import { getDecoratePreview } from '#/api/decorate/page';

import NavFloatBall from '../nav/components/nav-float-ball.vue';
import NavNavbar from '../nav/components/nav-navbar.vue';
import NavTabbar from '../nav/components/nav-tabbar.vue';
import { parseNavConfig } from '../nav/config';
import { getWidget, normalizeSchema } from '../widgets';

const route = useRoute();
const code = computed(() => String(route.params.code || ''));
const loading = ref(true);
const error = ref('');
const pageName = ref('');
const nav = ref<NavConfig | null>(null);
const schema = ref<PageSchema>({
  components: [],
  page: { bgColor: '#f5f6f7', bgImage: '', padding: 0, title: '' },
});

const visibleComponents = computed(() =>
  schema.value.components.filter(
    (item) => item.visible !== false && getWidget(item.type),
  ),
);

/** 悬浮球要浮在 TabBar 之上 */
const floatBottom = computed(() => (nav.value?.tabbar.enabled ? 64 : 16));

onMounted(async () => {
  // 导航配置与组件库同源，页面渲染不因导航读取失败而白屏
  const [page, navResult] = await Promise.allSettled([
    getDecoratePreview(code.value),
    getPreviewDecorateNav(),
  ]);

  if (page.status === 'fulfilled') {
    pageName.value = page.value.name;
    schema.value = normalizeSchema(
      JSON.parse(page.value.schemaJson),
      page.value.name,
    );
  } else {
    error.value = (page.reason as any)?.message || '预览加载失败';
  }

  nav.value = parseNavConfig(
    navResult.status === 'fulfilled' ? navResult.value : null,
  );
  loading.value = false;
});
</script>

<template>
  <div class="preview-root">
    <div v-if="loading" class="state">正在加载页面…</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else class="phone">
      <!-- 顶部导航（与后台「导航装修」一致，支持沉浸式叠加） -->
      <NavNavbar v-if="nav" :navbar="nav.navbar" sticky />

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

      <!-- 底部 TabBar -->
      <NavTabbar v-if="nav" class="sticky-tabbar" :tabbar="nav.tabbar" />

      <!-- 悬浮快捷入口 -->
      <NavFloatBall
        v-if="nav"
        :bottom="floatBottom"
        :float-ball="nav.floatBall"
        fixed
      />
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
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  background: #fff;
  box-shadow: 0 0 24px rgb(0 0 0 / 8%);
}
.screen {
  flex: 1;
}
.sticky-tabbar {
  position: sticky;
  bottom: 0;
  z-index: 4;
  box-shadow: 0 -1px 6px rgb(0 0 0 / 6%);
}
.footer {
  padding: 12px 0 20px;
  font-size: 11px;
  color: #b1b3b8;
}
</style>
