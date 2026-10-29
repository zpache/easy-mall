<script lang="ts" setup>
import type { MaterialApi } from '#/api/decorate/material';
import type { UploadRequestOptions } from 'element-plus';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElUpload,
} from 'element-plus';

import { deleteMaterial, getMaterials, renameMaterial } from '#/api/decorate/material';
import { uploadImageApi } from '#/api/upload';

interface MaterialItem extends MaterialApi.Material {
  /** 上传完成后本地补充 */
  uploading?: boolean;
}

const loading = ref(false);
const uploading = ref(false);
const records = ref<MaterialItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(24);
const keyword = ref('');
const mediaType = ref('');

const MEDIA_OPTIONS = [
  { label: '全部类型', value: '' },
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
];

function formatSize(size: number) {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

async function load() {
  loading.value = true;
  try {
    const result = await getMaterials({
      keyword: keyword.value || undefined,
      mediaType: mediaType.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    records.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  pageNum.value = 1;
  load();
}

/** el-upload 自定义上传：复用 COS 直传 / 本地转存链路，上传即入素材库 */
async function doUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    await uploadImageApi(options.file, { scene: 'decorate' });
    ElMessage.success('上传成功');
    pageNum.value = 1;
    await load();
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败');
    throw error;
  } finally {
    uploading.value = false;
  }
}

async function onRename(item: MaterialItem) {
  const { value } = await ElMessageBox.prompt('修改素材名称', '重命名', {
    inputValue: item.name,
    inputPattern: /\S+/,
    inputErrorMessage: '名称不能为空',
  });
  await renameMaterial(item.id, value.trim());
  ElMessage.success('已重命名');
  await load();
}

async function onDelete(item: MaterialItem) {
  await ElMessageBox.confirm(
    `确定删除「${item.name}」吗？仅移除素材库记录，不会清理已上传的文件。`,
    '删除素材',
    { type: 'warning' },
  );
  await deleteMaterial(item.id);
  ElMessage.success('已删除');
  await load();
}

async function onCopy(item: MaterialItem) {
  await navigator.clipboard.writeText(item.url);
  ElMessage.success('链接已复制');
}

function preview(item: MaterialItem) {
  window.open(item.url, '_blank');
}

onMounted(load);
</script>

<template>
  <Page title="素材中心" description="管理上传的图片素材，装修组件可直接从素材库选用">
    <ElCard shadow="never">
      <div class="toolbar">
        <ElInput
          v-model="keyword"
          class="toolbar__search"
          clearable
          placeholder="搜索素材名称 / 文件名"
          @keyup.enter="search"
          @clear="search"
        />
        <ElSelect
          v-model="mediaType"
          class="toolbar__type"
          @change="search"
        >
          <ElOption
            v-for="opt in MEDIA_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </ElSelect>
        <ElButton :loading="loading" @click="search">查询</ElButton>
        <ElUpload
          :http-request="doUpload"
          :show-file-list="false"
          accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
        >
          <ElButton :loading="uploading" type="primary">上传素材</ElButton>
        </ElUpload>
      </div>

      <ElEmpty v-if="!loading && records.length === 0" description="还没有素材，点击右上角上传" />

      <div v-loading="loading" class="grid">
        <div v-for="item in records" :key="item.id" class="cell">
          <div class="cell__preview" @click="preview(item)">
            <img :src="item.url" :alt="item.name" loading="lazy" />
          </div>
          <div class="cell__name" :title="item.name">{{ item.name }}</div>
          <div class="cell__meta">{{ formatSize(item.size) }} · {{ item.createdAt?.slice(0, 10) }}</div>
          <div class="cell__actions">
            <ElButton size="small" text type="primary" @click="onCopy(item)">复制链接</ElButton>
            <ElButton size="small" text @click="onRename(item)">重命名</ElButton>
            <ElButton size="small" text type="danger" @click="onDelete(item)">删除</ElButton>
          </div>
        </div>
      </div>

      <div v-if="total > pageSize" class="pager">
        <ElPagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[24, 48, 96]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </ElCard>
  </Page>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar__search {
  width: 240px;
}

.toolbar__type {
  width: 120px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.cell {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.cell__preview {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 130px;
  overflow: hidden;
  cursor: pointer;
  background: var(--el-fill-color-lighter);
}

.cell__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cell__name {
  margin: 8px 10px 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell__meta {
  margin: 2px 10px 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.cell__actions {
  display: flex;
  align-items: center;
  margin: 4px 4px 6px;

  :deep(.el-button + .el-button) {
    margin-left: 2px;
  }
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
