<script lang="ts" setup>
import type { DecorateNavApi as NavApi } from '#/api/decorate/nav';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElCard,
  ElColorPicker,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElSwitch,
  ElTag,
} from 'element-plus';

import { getDecoratePages } from '#/api/decorate/page';
import {
  getDecorateNav,
  resetDecorateNav,
  saveDecorateNav,
} from '#/api/decorate/nav';

import IconField from './components/icon-field.vue';
import PhonePreview from './components/phone-preview.vue';
import {
  createDefaultNav,
  FLOAT_ICONS,
  MAX_FLOAT,
  MAX_TABS,
  MIN_TABS,
  parseNavConfig,
  tabItem as TabItem,
} from './config';

defineOptions({ name: 'DecorateNav' });

const LINK_TYPES = [
  { label: '商城首页', value: 'home' },
  { label: '自定义页面', value: 'page' },
  { label: '自定义路径', value: 'path' },
  { label: '不跳转', value: 'none' },
];

const BORDER_STYLES = [
  { label: '黑色细线', value: 'black' },
  { label: '白色细线', value: 'white' },
  { label: '不显示', value: 'none' },
];

const BACK_BEHAVIORS = [
  { label: '返回上一页', value: 'back' },
  { label: '回到首页', value: 'home' },
  { label: '不响应', value: 'none' },
];

const initial = createDefaultNav();

const nav = reactive({
  floatBall: initial.floatBall,
  navbar: initial.navbar,
  tabbar: initial.tabbar,
});

const version = ref<number>();
const updatedAt = ref('');
const loading = ref(false);
const saving = ref(false);
const dirty = ref(false);
const pages = ref<{ id: number; name: string }[]>([]);

/** 用户改动后标记未保存，保存/重置后清除 */
function touch() {
  dirty.value = true;
}

function applyConfig(config: NavApi.Config) {
  const parsedNav = parseNavConfig(config);
  nav.tabbar = parsedNav.tabbar;
  nav.navbar = parsedNav.navbar;
  nav.floatBall = parsedNav.floatBall;
  version.value = config.version;
  updatedAt.value = config.updatedAt ?? '';
  dirty.value = false;
}

async function load() {
  loading.value = true;
  try {
    applyConfig(await getDecorateNav());
  } finally {
    loading.value = false;
  }
}

async function loadPages() {
  const result = await getDecoratePages({ pageNum: 1, pageSize: 100 });
  pages.value = result.items.map((item) => ({ id: item.id, name: item.name }));
}

onMounted(async () => {
  await Promise.all([load(), loadPages()]);
});

/* ---------------- TabBar ---------------- */

function addTab() {
  if (nav.tabbar.items.length >= MAX_TABS) {
    ElMessage.warning(`最多 ${MAX_TABS} 个标签`);
    return;
  }
  nav.tabbar.items.push(
    TabItem(`标签${nav.tabbar.items.length + 1}`, `tab_${Date.now().toString(36)}`, 'none'),
  );
  touch();
}

function removeTab(index: number) {
  if (nav.tabbar.items.length <= MIN_TABS) {
    ElMessage.warning(`至少保留 ${MIN_TABS} 个标签`);
    return;
  }
  nav.tabbar.items.splice(index, 1);
  touch();
}

function moveTab(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= nav.tabbar.items.length) {
    return;
  }
  const [item] = nav.tabbar.items.splice(index, 1);
  nav.tabbar.items.splice(target, 0, item!);
  touch();
}

function onLinkTypeChange(item: NavApi.TabItem) {
  item.linkValue = '';
  touch();
}

function pageName(id: string) {
  return pages.value.find((page) => String(page.id) === id)?.name ?? `页面 ${id}`;
}

/* ---------------- 快捷入口 ---------------- */

function addFloatItem() {
  if (nav.floatBall.itemList.length >= MAX_FLOAT) {
    ElMessage.warning(`最多 ${MAX_FLOAT} 个快捷入口`);
    return;
  }
  nav.floatBall.itemList.push({
    enabled: true,
    icon: '',
    key: `custom_${Date.now().toString(36)}`,
    name: '新入口',
    value: '',
  });
  touch();
}

function removeFloatItem(index: number) {
  nav.floatBall.itemList.splice(index, 1);
  touch();
}

function moveFloatItem(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= nav.floatBall.itemList.length) {
    return;
  }
  const [item] = nav.floatBall.itemList.splice(index, 1);
  nav.floatBall.itemList.splice(target, 0, item!);
  touch();
}

function floatIcon(item: NavApi.FloatItem) {
  return item.icon || FLOAT_ICONS[item.key] || 'lucide:circle-dot';
}

/* ---------------- 保存 / 重置 ---------------- */

const tabCountTip = computed(
  () => `${nav.tabbar.items.length}/${MAX_TABS} 个标签`,
);

async function onSave() {
  if (nav.navbar.title.trim().length === 0) {
    ElMessage.warning('请填写顶部导航标题');
    return;
  }
  const emptyName = nav.tabbar.items.find((item) => !item.name.trim());
  if (emptyName) {
    ElMessage.warning('每个 TabBar 标签都需要填写文字');
    return;
  }
  saving.value = true;
  try {
    const result = await saveDecorateNav({
      floatJson: JSON.stringify(nav.floatBall),
      navbarJson: JSON.stringify(nav.navbar),
      tabbarJson: JSON.stringify(nav.tabbar),
      version: version.value,
    });
    version.value = result.version;
    updatedAt.value = result.updatedAt ?? '';
    dirty.value = false;
    ElMessage.success('导航配置已保存，小程序下次启动即生效');
  } finally {
    saving.value = false;
  }
}

async function onReset() {
  await ElMessageBox.confirm(
    '将恢复为默认导航（4 个标签、常规标题栏、右侧快捷入口），当前配置会被覆盖。',
    '恢复默认配置',
    { type: 'warning' },
  );
  loading.value = true;
  try {
    applyConfig(await resetDecorateNav());
    ElMessage.success('已恢复默认配置');
  } finally {
    loading.value = false;
  }
}

const previewPageName = computed(() => {
  const first = nav.tabbar.items[0];
  return first?.linkType === 'page' && first.linkValue
    ? pageName(first.linkValue)
    : '商城首页';
});
</script>

<template>
  <Page auto-content-height>
    <div class="nav-page" v-loading="loading">
      <div class="nav-main">
        <!-- 头部操作条 -->
        <div class="head">
          <div class="head-left">
            <h2>导航装修</h2>
            <p>
              配置小程序全局的底部 TabBar、顶部标题栏与悬浮快捷入口；改动即时预览，保存后下发到小程序。
            </p>
          </div>
          <div class="head-right">
            <ElTag v-if="dirty" effect="plain" type="warning">未保存</ElTag>
            <ElTag v-else-if="updatedAt" effect="plain" type="info">
              上次保存 {{ updatedAt.replace('T', ' ').slice(0, 19) }}
            </ElTag>
            <ElButton :loading="loading" @click="load">重新加载</ElButton>
            <ElButton :disabled="saving" @click="onReset">恢复默认</ElButton>
            <ElButton :loading="saving" type="primary" @click="onSave">保存配置</ElButton>
          </div>
        </div>

        <!-- 底部 TabBar -->
        <ElCard class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span class="section-title">
                <IconifyIcon icon="lucide:panel-bottom" />
                底部 TabBar
              </span>
              <div class="section-ops">
                <span class="tip">3~5 个标签</span>
                <ElSwitch v-model="nav.tabbar.enabled" size="small" @change="touch" />
              </div>
            </div>
          </template>

          <div v-show="nav.tabbar.enabled" class="section-body">
            <div class="row">
              <div class="field">
                <label>背景色</label>
                <ElColorPicker v-model="nav.tabbar.bgColor" size="small" @change="touch" />
              </div>
              <div class="field">
                <label>未选中文字</label>
                <ElColorPicker v-model="nav.tabbar.color" size="small" @change="touch" />
              </div>
              <div class="field">
                <label>选中文字</label>
                <ElColorPicker v-model="nav.tabbar.selectedColor" size="small" @change="touch" />
              </div>
              <div class="field wide">
                <label>上边框</label>
                <ElSelect v-model="nav.tabbar.borderStyle" size="small" @change="touch">
                  <ElOption
                    v-for="item in BORDER_STYLES"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </ElSelect>
              </div>
            </div>

            <div class="tabs-head">
              <span>标签配置</span>
              <span class="tip">{{ tabCountTip }}</span>
            </div>

            <div class="tab-list">
              <div v-for="(item, index) in nav.tabbar.items" :key="item.key" class="tab-card">
                <div class="tab-card-head">
                  <span class="index">第 {{ index + 1 }} 个</span>
                  <ElInput
                    v-model="item.name"
                    maxlength="8"
                    placeholder="标签文字（最多 8 字）"
                    size="small"
                    style="width: 180px"
                    @input="touch"
                  />
                  <span class="spacer" />
                  <ElButton size="small" text @click="moveTab(index, -1)">
                    <IconifyIcon icon="lucide:chevron-up" />
                  </ElButton>
                  <ElButton size="small" text @click="moveTab(index, 1)">
                    <IconifyIcon icon="lucide:chevron-down" />
                  </ElButton>
                  <ElButton size="small" text type="danger" @click="removeTab(index)">
                    <IconifyIcon icon="lucide:trash-2" />
                  </ElButton>
                </div>

                <div class="tab-card-body">
                  <div class="field-col">
                    <label>未选中图标</label>
                    <IconField
                      v-model="item.icon"
                      placeholder-icon="lucide:circle"
                      @update:model-value="touch"
                    />
                  </div>
                  <div class="field-col">
                    <label>选中态图标</label>
                    <IconField
                      v-model="item.selectedIcon"
                      placeholder-icon="lucide:circle-dot"
                      @update:model-value="touch"
                    />
                  </div>
                </div>

                <div class="row">
                  <div class="field">
                    <label>跳转</label>
                    <ElSelect
                      v-model="item.linkType"
                      size="small"
                      @change="onLinkTypeChange(item)"
                    >
                      <ElOption
                        v-for="type in LINK_TYPES"
                        :key="type.value"
                        :label="type.label"
                        :value="type.value"
                      />
                    </ElSelect>
                  </div>
                  <div v-if="item.linkType === 'page'" class="field grow">
                    <label>自定义页面</label>
                    <ElSelect
                      v-model="item.linkValue"
                      placeholder="选择装修页面"
                      size="small"
                      @change="touch"
                    >
                      <ElOption
                        v-for="page in pages"
                        :key="page.id"
                        :label="page.name"
                        :value="String(page.id)"
                      />
                    </ElSelect>
                  </div>
                  <div v-else-if="item.linkType === 'path'" class="field grow">
                    <label>自定义路径</label>
                    <ElInput
                      v-model="item.linkValue"
                      placeholder="/pages/category/index"
                      size="small"
                      @input="touch"
                    />
                  </div>
                </div>
              </div>
            </div>

            <ElButton
              :disabled="nav.tabbar.items.length >= MAX_TABS"
              class="add-btn"
              plain
              size="small"
              @click="addTab"
            >
              <IconifyIcon class="mr-1" icon="lucide:plus" />
              添加标签
            </ElButton>
          </div>
        </ElCard>

        <!-- 顶部导航 -->
        <ElCard class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span class="section-title">
                <IconifyIcon icon="lucide:panel-top" />
                顶部导航
              </span>
              <span class="tip">标题、样式与返回行为</span>
            </div>
          </template>

          <div class="section-body">
            <div class="row">
              <div class="field grow">
                <label>标题</label>
                <ElInput
                  v-model="nav.navbar.title"
                  maxlength="20"
                  placeholder="显示在标题栏中间"
                  show-word-limit
                  size="small"
                  @input="touch"
                />
              </div>
              <div class="field wide">
                <label>样式</label>
                <ElRadioGroup
                  v-model="nav.navbar.style"
                  size="small"
                  @change="touch"
                >
                  <ElRadioButton label="normal">常规</ElRadioButton>
                  <ElRadioButton label="immersive">沉浸式透明</ElRadioButton>
                </ElRadioGroup>
              </div>
            </div>

            <div class="row">
              <div class="field">
                <label>背景色</label>
                <ElColorPicker
                  v-model="nav.navbar.bgColor"
                  :disabled="nav.navbar.style === 'immersive'"
                  size="small"
                  @change="touch"
                />
              </div>
              <div class="field">
                <label>文字色</label>
                <ElColorPicker
                  v-model="nav.navbar.textColor"
                  :disabled="nav.navbar.style === 'immersive'"
                  size="small"
                  @change="touch"
                />
              </div>
              <div class="field">
                <label>显示返回按钮</label>
                <ElSwitch v-model="nav.navbar.showBack" size="small" @change="touch" />
              </div>
              <div class="field wide">
                <label>返回按钮行为</label>
                <ElSelect
                  v-model="nav.navbar.backBehavior"
                  :disabled="!nav.navbar.showBack"
                  size="small"
                  @change="touch"
                >
                  <ElOption
                    v-for="item in BACK_BEHAVIORS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </ElSelect>
              </div>
            </div>
            <p class="note">
              沉浸式透明会把标题栏叠加在页面顶部（适合整屏图片头图），文字自动转白色并加阴影。
            </p>
          </div>
        </ElCard>

        <!-- 快捷入口 / 悬浮球 -->
        <ElCard class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span class="section-title">
                <IconifyIcon icon="lucide:circle-dot-dashed" />
                快捷入口 / 悬浮球
              </span>
              <div class="section-ops">
                <span class="tip">最多 {{ MAX_FLOAT }} 项</span>
                <ElSwitch v-model="nav.floatBall.enabled" size="small" @change="touch" />
              </div>
            </div>
          </template>

          <div v-show="nav.floatBall.enabled" class="section-body">
            <div class="row">
              <div class="field wide">
                <label>悬浮位置</label>
                <ElRadioGroup v-model="nav.floatBall.position" size="small" @change="touch">
                  <ElRadioButton label="left">左侧</ElRadioButton>
                  <ElRadioButton label="right">右侧</ElRadioButton>
                </ElRadioGroup>
              </div>
            </div>

            <div class="float-list">
              <div
                v-for="(item, index) in nav.floatBall.itemList"
                :key="item.key"
                class="float-card"
              >
                <div class="float-head">
                  <IconifyIcon class="float-icon" :icon="floatIcon(item)" />
                  <ElInput
                    v-model="item.name"
                    maxlength="8"
                    placeholder="名称"
                    size="small"
                    style="width: 120px"
                    @input="touch"
                  />
                  <ElInput
                    v-if="item.key === 'service'"
                    v-model="item.value"
                    placeholder="客服电话"
                    size="small"
                    style="width: 160px"
                    @input="touch"
                  />
                  <ElInput
                    v-else
                    :model-value="item.value"
                    disabled
                    placeholder="无需额外配置"
                    size="small"
                    style="width: 160px"
                  />
                  <span class="spacer" />
                  <ElSwitch v-model="item.enabled" size="small" @change="touch" />
                  <ElButton size="small" text @click="moveFloatItem(index, -1)">
                    <IconifyIcon icon="lucide:chevron-up" />
                  </ElButton>
                  <ElButton size="small" text @click="moveFloatItem(index, 1)">
                    <IconifyIcon icon="lucide:chevron-down" />
                  </ElButton>
                  <ElButton size="small" text type="danger" @click="removeFloatItem(index)">
                    <IconifyIcon icon="lucide:trash-2" />
                  </ElButton>
                </div>
                <IconField
                  v-model="item.icon"
                  :placeholder-icon="floatIcon(item)"
                  @update:model-value="touch"
                />
              </div>
            </div>

            <ElButton
              :disabled="nav.floatBall.itemList.length >= MAX_FLOAT"
              class="add-btn"
              plain
              size="small"
              @click="addFloatItem"
            >
              <IconifyIcon class="mr-1" icon="lucide:plus" />
              添加入口
            </ElButton>
          </div>
        </ElCard>
      </div>

      <!-- 右侧实时预览 -->
      <div class="nav-preview">
        <div class="preview-title">实时预览</div>
        <PhonePreview
          :float-ball="nav.floatBall"
          :navbar="nav.navbar"
          :page-name="previewPageName"
          :tabbar="nav.tabbar"
        />
        <div class="preview-tips">
          <p>· TabBar 首个标签为选中态展示</p>
          <p>· 沉浸式样式下标题栏叠加在页头图之上</p>
          <p>· 图标留空时使用内置线性图标</p>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.nav-page {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  height: 100%;
  padding: 12px;
  overflow: hidden;
  background: #f5f7fa;
}
.nav-main {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding-right: 4px;
  overflow-y: auto;
}
.head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
}
.head h2 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #1f2329;
}
.head p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
.head-right {
  display: flex;
  flex: none;
  gap: 8px;
  align-items: center;
}
.section {
  margin-bottom: 12px;
  border-radius: 8px;
}
.section :deep(.el-card__header) {
  padding: 12px 16px;
  background: #fafbfc;
}
.section :deep(.el-card__body) {
  padding: 14px 16px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section-title {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.section-ops {
  display: flex;
  gap: 10px;
  align-items: center;
}
.tip {
  font-size: 11px;
  color: #a8abb2;
}
.section-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 128px;
}
.field.wide {
  min-width: 180px;
}
.field.grow {
  flex: 1;
  min-width: 200px;
}
.field label,
.field-col label {
  font-size: 12px;
  color: #606266;
}
.field-col {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 240px;
}
.tabs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 4px;
  font-size: 12px;
  color: #606266;
}
.tab-list,
.float-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tab-card,
.float-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.tab-card-head,
.float-head {
  display: flex;
  gap: 6px;
  align-items: center;
}
.tab-card-body {
  display: flex;
  gap: 16px;
}
.index {
  width: 46px;
  font-size: 11px;
  color: #909399;
}
.spacer {
  flex: 1;
}
.float-icon {
  font-size: 16px;
  color: #606266;
}
.add-btn {
  align-self: flex-start;
}
.note {
  margin: 0;
  font-size: 11px;
  color: #a8abb2;
}
.nav-preview {
  flex: none;
  width: 372px;
  padding: 14px;
  background: #fff;
  border-radius: 8px;
}
.preview-title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
  text-align: center;
}
.preview-tips {
  margin-top: 14px;
  font-size: 11px;
  line-height: 1.8;
  color: #a8abb2;
}
.preview-tips p {
  margin: 0;
}
</style>
