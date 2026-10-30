<script lang="ts" setup>
import type { NavConfig } from '../nav/config';

import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAccessStore } from '@vben/stores';
import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElTag,
  ElTooltip,
} from 'element-plus';

import { getDecorateNav } from '#/api/decorate/nav';

import { parseNavConfig } from '../nav/config';
import { useDecorateEditor } from './use-editor';
import PhoneCanvas from './components/phone-canvas.vue';
import PreviewQrcode from './components/preview-qrcode.vue';
import PropPanel from './components/prop-panel.vue';
import WidgetPanel from './components/widget-panel.vue';

const route = useRoute();
const router = useRouter();

const pageId = computed(() => Number(route.params.id));

const editor = useDecorateEditor(() => pageId.value);
const {
  canRedo,
  canUndo,
  components,
  dirty,
  duplicateWidget,
  endDrag,
  loading,
  moveBy,
  pageMeta,
  pageName,
  publish,
  redo,
  removeWidget,
  save,
  saving,
  schema,
  select,
  selected,
  selectedId,
  startDrag,
  toggleVisible,
  undo,
  updatePageConfig,
  updateProps,
  addWidget,
  load,
} = editor;

const previewVisible = ref(false);
const nameEditing = ref(false);

/* ---------------- 导航联动 ---------------- */

const NAV_VISIBLE_KEY = 'easymall:decorate:show-nav';

const navConfig = ref<NavConfig | null>(null);
const showNav = ref(localStorage.getItem(NAV_VISIBLE_KEY) !== '0');

watch(showNav, (value) => {
  localStorage.setItem(NAV_VISIBLE_KEY, value ? '1' : '0');
});

/** 读取后台「导航装修」配置，画布底部按同一份配置渲染 */
async function loadNav() {
  try {
    navConfig.value = parseNavConfig(await getDecorateNav());
  } catch {
    // 导航配置读取失败不该阻塞页面装修，退回内置默认值
    navConfig.value = parseNavConfig(null);
  }
}

function openNavSettings() {
  window.open(router.resolve('/decorate/nav').href, '_blank');
}

const saveStateText = computed(() => {
  if (saving.value) {
    return '保存中…';
  }
  return dirty.value ? '未保存' : '已保存';
});

const saveStateType = computed(() => {
  if (saving.value) {
    return 'warning';
  }
  return dirty.value ? 'info' : 'success';
});

function onDragStart(type: string) {
  startDrag({ kind: 'new', type });
}

function onItemDragStart(index: number) {
  startDrag({ fromIndex: index, kind: 'move' });
}

function onDrop(index: number) {
  editor.dropAt(index);
}

function onAdd(type: string) {
  addWidget(type);
}

/** 发布直接影响小程序端展示，必须二次确认 */
async function onPublish() {
  await ElMessageBox.confirm(
    `发布后「${pageName.value}」的装修内容将立即对小程序端生效，确定发布吗？`,
    '发布页面',
    { type: 'warning', confirmButtonText: '确认发布' },
  );
  try {
    await publish();
  } catch (error: any) {
    ElMessage.error(error?.message || '发布失败');
  }
}

async function onBack() {
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('当前页面有未保存的修改，是否保存后离开？', '提示', {
        confirmButtonText: '保存并离开',
        cancelButtonText: '直接离开',
        distinguishCancelAndClose: true,
        type: 'warning',
      });
      await save(true);
    } catch (action) {
      if (action === 'close') {
        return;
      }
    }
  }
  router.push('/decorate/list');
}

async function onRename(value: string) {
  if (!value.trim()) {
    return;
  }
  editor.rename(value.trim());
}

onMounted(async () => {
  // 编辑器挂载在 coreRoutes（后端路由模式下静态路由不生效），登录态自行校验
  const accessStore = useAccessStore();
  if (!accessStore.accessToken) {
    await router.replace({
      path: '/auth/login',
      query: { redirect: encodeURIComponent(route.fullPath) },
    });
    return;
  }
  await Promise.all([load(), loadNav()]);
});
</script>

<template>
  <div class="editor-root">
    <header class="editor-toolbar">
      <div class="left">
        <ElButton text size="small" @click="onBack">
          <IconifyIcon icon="lucide:arrow-left" class="mr-1" />
          返回
        </ElButton>
        <span class="divider" />
        <ElInput
          v-if="nameEditing"
          v-model="pageName"
          class="name-input"
          size="small"
          @blur="
            nameEditing = false;
            onRename(pageName);
          "
          @keyup.enter="
            nameEditing = false;
            onRename(pageName);
          "
        />
        <span v-else class="page-name" @click="nameEditing = true">
          {{ pageName || '未命名页面' }}
          <IconifyIcon icon="lucide:pencil" class="edit-icon" />
        </span>
        <ElTag :type="saveStateType" size="small" effect="light">
          {{ saveStateText }}
        </ElTag>
      </div>

      <div class="right">
        <ElTooltip content="撤销 (⌘Z)" placement="bottom">
          <ElButton :disabled="!canUndo" size="small" @click="undo">
            <IconifyIcon icon="lucide:undo-2" />
          </ElButton>
        </ElTooltip>
        <ElTooltip content="重做 (⇧⌘Z)" placement="bottom">
          <ElButton :disabled="!canRedo" size="small" @click="redo">
            <IconifyIcon icon="lucide:redo-2" />
          </ElButton>
        </ElTooltip>
        <span class="divider" />
        <ElButton size="small" @click="previewVisible = true">
          <IconifyIcon icon="lucide:qr-code" class="mr-1" />
          扫码预览
        </ElButton>
        <ElButton :loading="saving" size="small" @click="save(false)">
          保存草稿
        </ElButton>
        <ElButton size="small" type="primary" @click="onPublish">
          <IconifyIcon icon="lucide:upload" class="mr-1" />
          发布
        </ElButton>
      </div>
    </header>

    <main class="editor-main">
      <WidgetPanel
        @add="onAdd"
        @dragend="endDrag"
        @dragstart="onDragStart"
      />
      <PhoneCanvas
        :nav="navConfig"
        :schema="schema"
        :selected-id="selectedId"
        :show-nav="showNav"
        @clear="select(null)"
        @duplicate="duplicateWidget"
        @dragend="endDrag"
        @dragstart="onItemDragStart"
        @drop="onDrop"
        @move="moveBy"
        @open-nav="openNavSettings"
        @remove="removeWidget"
        @select="select"
        @toggle="toggleVisible"
        @update:show-nav="showNav = $event"
      />
      <div v-if="loading" class="loading-mask">
        <IconifyIcon icon="lucide:loader-2" class="spin" />
        正在加载页面…
      </div>
      <PropPanel
        :component-count="components.length"
        :page="schema.page"
        :selected="selected"
        @update-page="updatePageConfig"
        @update-props="
          (patch) => selectedId && updateProps(selectedId, patch)
        "
      />
    </main>

    <PreviewQrcode
      v-model:visible="previewVisible"
      :page-id="pageId"
      :page-name="pageName"
      :preview-code="pageMeta?.previewCode"
    />
  </div>
</template>

<style scoped>
.editor-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: #f5f6f7;
}
.editor-toolbar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.left,
.right {
  display: flex;
  gap: 8px;
  align-items: center;
}
.divider {
  width: 1px;
  height: 16px;
  margin: 0 4px;
  background: #ebeef5;
}
.page-name {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 2px 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
  cursor: pointer;
  border-radius: 4px;
}
.page-name:hover {
  background: #f5f7fa;
}
.edit-icon {
  font-size: 11px;
  color: #a8abb2;
}
.name-input {
  width: 200px;
}
.editor-main {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
}
.loading-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #606266;
  background: rgb(255 255 255 / 70%);
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
