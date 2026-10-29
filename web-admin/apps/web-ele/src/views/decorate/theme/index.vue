<script lang="ts" setup>
import type { DecorateThemeApi } from '#/api/decorate/theme';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElColorPicker,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElTag,
} from 'element-plus';

import {
  activateDecorateTheme,
  createDecorateTheme,
  deleteDecorateTheme,
  getDecorateThemes,
  updateDecorateTheme,
} from '#/api/decorate/theme';

type ThemeItem = DecorateThemeApi.Theme;

const DEFAULT_CONFIG: DecorateThemeApi.ThemeConfig = {
  pageBgColor: '#f5f6f7',
  priceColor: '#ff4d1f',
  primaryColor: '#ff6b00',
  subTextColor: '#999999',
  textColor: '#1f1f1f',
};

const COLOR_FIELDS: Array<{
  key: keyof DecorateThemeApi.ThemeConfig;
  label: string;
  tip: string;
}> = [
  { key: 'primaryColor', label: '主色', tip: '按钮、强调、选中态' },
  { key: 'priceColor', label: '价格色', tip: '商品价格强调' },
  { key: 'pageBgColor', label: '页面背景', tip: '装修页面整体底色' },
  { key: 'textColor', label: '标题文字', tip: '标题与主要文字' },
  { key: 'subTextColor', label: '次要文字', tip: '副标题、描述文字' },
];

const themes = ref<ThemeItem[]>([]);
const loading = ref(false);
const saving = ref(false);
/** null 表示正在新建（未落库） */
const editingId = ref<number | null>(null);
const editingName = ref('');
const editingConfig = ref<DecorateThemeApi.ThemeConfig>({ ...DEFAULT_CONFIG });
const dirty = ref(false);

/** 已启用主题：线上生效的那份（读发布快照） */
const activeTheme = computed(() => themes.value.find((t) => t.isActive === 1));

async function load() {
  loading.value = true;
  try {
    themes.value = await getDecorateThemes();
    // 默认选中线上主题；没有则选第一个
    const prefer = activeTheme.value ?? themes.value[0];
    if (prefer && editingId.value === null && !dirty.value) {
      select(prefer);
    }
  } finally {
    loading.value = false;
  }
}

function select(theme: ThemeItem) {
  editingId.value = theme.id;
  editingName.value = theme.name;
  editingConfig.value = { ...DEFAULT_CONFIG, ...JSON.parse(theme.configJson) };
  dirty.value = false;
}

function startCreate() {
  editingId.value = null;
  editingName.value = '新主题';
  editingConfig.value = { ...DEFAULT_CONFIG };
  dirty.value = true;
}

function markDirty() {
  dirty.value = true;
}

async function onSave() {
  if (!editingName.value.trim()) {
    ElMessage.warning('请填写主题名称');
    return;
  }
  saving.value = true;
  try {
    const payload: DecorateThemeApi.SavePayload = {
      configJson: JSON.stringify(editingConfig.value),
      name: editingName.value.trim(),
    };
    if (editingId.value === null) {
      const created = await createDecorateTheme(payload);
      ElMessage.success('主题已创建，点「启用」发布到小程序');
      await load();
      select(created);
    } else {
      payload.version = themes.value.find((t) => t.id === editingId.value)?.version;
      await updateDecorateTheme(editingId.value, payload);
      ElMessage.success('草稿已保存，点「启用」发布到小程序');
      dirty.value = false;
      await load();
    }
  } finally {
    saving.value = false;
  }
}

async function onActivate(theme: ThemeItem) {
  await ElMessageBox.confirm(
    `确定把「${theme.name}」发布为线上主题吗？小程序端将整体换肤。`,
    '启用主题',
    { type: 'warning' },
  );
  await activateDecorateTheme(theme.id);
  ElMessage.success('已启用，小程序端下次进入即生效');
  await load();
  const fresh = themes.value.find((t) => t.id === theme.id);
  if (fresh) select(fresh);
}

async function onDelete(theme: ThemeItem) {
  await ElMessageBox.confirm(`确定删除「${theme.name}」吗？`, '删除主题', {
    type: 'warning',
  });
  await deleteDecorateTheme(theme.id);
  ElMessage.success('已删除');
  if (editingId.value === theme.id) {
    editingId.value = null;
  }
  await load();
}

/** 编辑器与手机预览共用同一份配置（草稿态预览） */
function colorCss(key: keyof DecorateThemeApi.ThemeConfig) {
  return editingConfig.value[key] || DEFAULT_CONFIG[key];
}

onMounted(load);
</script>

<template>
  <Page title="主题换肤" description="全局配色一键换肤：装修页面与组件强调色随线上主题生效，启用即发布">
    <div class="theme-layout">
      <!-- 左侧：主题列表 -->
      <ElCard shadow="never" class="theme-list">
        <template #header>
          <div class="list-header">
            <span>主题列表</span>
            <ElButton size="small" type="primary" @click="startCreate">
              新建主题
            </ElButton>
          </div>
        </template>
        <div
          v-for="theme in themes"
          :key="theme.id"
          class="theme-item"
          :class="{ active: editingId === theme.id }"
          @click="select(theme)"
        >
          <div class="theme-item__swatches">
            <span
              v-for="key in ['primaryColor', 'priceColor', 'pageBgColor']"
              :key="key"
              class="swatch"
              :style="{
                background: (JSON.parse(theme.configJson) as DecorateThemeApi.ThemeConfig)[
                  key as keyof DecorateThemeApi.ThemeConfig
                ],
              }"
            />
          </div>
          <div class="theme-item__info">
            <div class="theme-item__name">
              {{ theme.name }}
              <ElTag v-if="theme.isActive === 1" size="small" type="success">
                线上
              </ElTag>
            </div>
            <div class="theme-item__time">{{ theme.updatedAt?.slice(0, 16) }}</div>
          </div>
          <div class="theme-item__actions" @click.stop>
            <ElButton
              v-if="theme.isActive !== 1"
              size="small"
              type="primary"
              @click="onActivate(theme)"
            >
              启用
            </ElButton>
            <ElButton
              v-if="theme.isActive !== 1"
              size="small"
              text
              type="danger"
              @click="onDelete(theme)"
            >
              删除
            </ElButton>
          </div>
        </div>
      </ElCard>

      <!-- 右侧：编辑器 + 预览 -->
      <ElCard shadow="never" class="theme-editor">
        <template #header>
          <div class="list-header">
            <span>
              {{ editingId === null ? '新建主题' : '编辑主题' }}
              <ElTag v-if="activeTheme && editingId === activeTheme.id" class="ml-2" size="small">
                当前线上主题（改动需「启用」后生效）
              </ElTag>
            </span>
            <ElButton :loading="saving" type="primary" @click="onSave">
              保存草稿
            </ElButton>
          </div>
        </template>

        <ElAlert
          class="editor-tip"
          :closable="false"
          show-icon
          title="保存草稿不影响线上；点左侧「启用」后草稿快照发布，小程序端整体换肤"
          type="info"
        />

        <div class="editor-body">
          <div class="editor-fields">
            <div class="name-row">
              <span class="field-label">主题名称</span>
              <ElInput
                v-model="editingName"
                class="name-input"
                maxlength="50"
                @input="markDirty"
              />
            </div>
            <div v-for="field in COLOR_FIELDS" :key="field.key" class="color-row">
              <div class="field-label">
                {{ field.label }}
                <div class="field-tip">{{ field.tip }}</div>
              </div>
              <ElColorPicker
                :model-value="colorCss(field.key)"
                @update:model-value="
                  (val: string | null) => {
                    editingConfig[field.key] = val || DEFAULT_CONFIG[field.key];
                    markDirty();
                  }
                "
              />
              <span class="color-value">{{ colorCss(field.key) }}</span>
            </div>
          </div>

          <!-- 手机预览（草稿态实时预览） -->
          <div class="phone">
            <div class="phone__screen" :style="{ background: colorCss('pageBgColor') }">
              <div class="phone__nav" :style="{ color: colorCss('textColor') }">
                易商城
              </div>
              <div
                class="phone__search"
                :style="{ background: colorCss('primaryColor'), color: '#fff' }"
              >
                🔍 搜索商品
              </div>
              <div class="phone__banner" :style="{ background: colorCss('primaryColor') }">
                <span>主题预览 Banner</span>
              </div>
              <div class="phone__goods">
                <div
                  v-for="n in 2"
                  :key="n"
                  class="phone__card"
                  :style="{ background: '#fff' }"
                >
                  <div
                    class="phone__img"
                    :style="{ background: colorCss('pageBgColor') }"
                  />
                  <div
                    class="phone__title"
                    :style="{ color: colorCss('textColor') }"
                  >
                    当季赣南脐橙
                  </div>
                  <div class="phone__sub" :style="{ color: colorCss('subTextColor') }">
                    已售 1280
                  </div>
                  <div class="phone__price">
                    <span :style="{ color: colorCss('priceColor') }">¥29.90</span>
                    <span
                      class="phone__btn"
                      :style="{ background: colorCss('primaryColor') }"
                    >
                      购买
                    </span>
                  </div>
                </div>
              </div>
              <div
                class="phone__tabbar"
                :style="{ color: colorCss('subTextColor') }"
              >
                <span :style="{ color: colorCss('primaryColor') }">首页</span>
                <span>分类</span>
                <span>购物车</span>
                <span>我的</span>
              </div>
            </div>
            <div class="phone__caption">草稿实时预览</div>
          </div>
        </div>
      </ElCard>
    </div>
  </Page>
</template>

<style scoped>
.theme-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  align-items: start;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.theme-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.theme-item:hover {
  border-color: var(--el-color-primary-light-5);
}

.theme-item.active {
  border-color: var(--el-color-primary);
}

.theme-item__swatches {
  display: flex;
  gap: 4px;
}

.swatch {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.theme-item__info {
  flex: 1;
  min-width: 0;
}

.theme-item__name {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
}

.theme-item__time {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.editor-tip {
  margin-bottom: 16px;
}

.editor-body {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.editor-fields {
  flex: 1;
}

.name-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}

.name-input {
  width: 240px;
}

.color-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.field-label {
  width: 150px;
  font-size: 14px;
  font-weight: 500;
}

.field-tip {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.color-value {
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 手机预览 */
.phone {
  flex-shrink: 0;
  text-align: center;
}

.phone__screen {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 220px;
  height: 400px;
  padding: 0 10px 10px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  box-shadow: 0 4px 16px rgb(0 0 0 / 8%);
}

.phone__nav {
  padding: 14px 0 6px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.phone__search {
  padding: 6px 10px;
  font-size: 12px;
  border-radius: 14px;
  opacity: 0.92;
}

.phone__banner {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 70px;
  font-size: 12px;
  color: #fff;
  border-radius: 10px;
  opacity: 0.9;
}

.phone__goods {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.phone__card {
  padding: 6px;
  border-radius: 8px;
}

.phone__img {
  height: 56px;
  border-radius: 6px;
}

.phone__title {
  margin-top: 4px;
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.phone__sub {
  font-size: 10px;
}

.phone__price {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
}

.phone__btn {
  padding: 2px 8px;
  font-size: 10px;
  color: #fff;
  border-radius: 10px;
}

.phone__tabbar {
  display: flex;
  justify-content: space-around;
  margin-top: auto;
  padding: 6px 0;
  font-size: 11px;
  background: #fff;
  border-radius: 8px;
}

.phone__caption {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
