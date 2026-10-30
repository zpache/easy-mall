<script lang="ts" setup>
import type { MaterialApi } from '#/api/decorate/material';
import type { UploadRequestOptions } from 'element-plus';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElImageViewer,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElUpload,
} from 'element-plus';

import { getMaterials } from '#/api/decorate/material';
import { uploadImageApi } from '#/api/upload';

defineOptions({ name: 'MaterialPicker' });

/**
 * 素材中心选择器：装修里所有需要图片的地方都走它，不再各自实现上传。
 * - 单选（默认）：点一下即回填并关闭；
 * - 多选（multiple）：勾选多张后确认，父组件一次性拿到地址数组（轮播图批量添加）；
 * - 弹窗内可直接上传，上传即登记素材库，保证素材统一来源。
 */
const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title?: string;
    /** 是否多选 */
    multiple?: boolean;
    /** 多选上限，0 表示不限制 */
    limit?: number;
    /** 当前已选中的地址：打开时回显高亮，继续选中时在其基础上累加 */
    selected?: string[];
    /** 素材类型过滤：image / video / audio，空为全部 */
    mediaType?: string;
    /** 只显示某个场景的素材，空为全部 */
    scene?: string;
    /** 弹窗内上传新素材时标记的场景，空则跟随 scene 过滤值 */
    uploadScene?: string;
  }>(),
  {
    limit: 0,
    mediaType: 'image',
    multiple: false,
    scene: '',
    selected: () => [],
    title: '从素材中心选择图片',
    uploadScene: '',
  },
);

const emit = defineEmits<{
  /** 多选确认：一次带回选中的所有地址 */
  confirm: [urls: string[]];
  /** 单选：选中即回调 */
  select: [url: string, item: MaterialApi.Material];
  'update:modelValue': [value: boolean];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
});

const loading = ref(false);
const uploading = ref(false);
const records = ref<MaterialApi.Material[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(18);
const keyword = ref('');
const mediaTypeFilter = ref(props.mediaType);
const sceneFilter = ref(props.scene);

/** 多选态：当前勾选的地址（打开时用父组件传入的已选值初始化） */
const picked = ref<string[]>([]);
const pickedCount = computed(() => picked.value.length);
const reachedLimit = computed(
  () => props.limit > 0 && picked.value.length >= props.limit,
);

/** 大图预览 */
const viewerUrl = ref('');

async function load() {
  loading.value = true;
  try {
    const result = await getMaterials({
      keyword: keyword.value || undefined,
      mediaType: mediaTypeFilter.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      scene: sceneFilter.value || undefined,
    });
    records.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (!val) {
      return;
    }
    picked.value = [...(props.selected || [])].filter(Boolean);
    mediaTypeFilter.value = props.mediaType;
    sceneFilter.value = props.scene;
    keyword.value = '';
    pageNum.value = 1;
    load();
  },
);

function search() {
  pageNum.value = 1;
  load();
}

function isPicked(url: string) {
  return picked.value.includes(url);
}

/** 缩略图右上角勾选/放大：多选加勾，单选直接回填；放大按钮阻止冒泡 */
function onCellClick(item: MaterialApi.Material) {
  if (!props.multiple) {
    emit('select', item.url, item);
    visible.value = false;
    return;
  }
  const index = picked.value.indexOf(item.url);
  if (index >= 0) {
    picked.value.splice(index, 1);
    return;
  }
  if (reachedLimit.value) {
    ElMessage.warning(`最多选择 ${props.limit} 张`);
    return;
  }
  picked.value.push(item.url);
}

function onConfirm() {
  if (props.multiple && picked.value.length === 0) {
    ElMessage.warning('请先选择素材');
    return;
  }
  emit('confirm', [...picked.value]);
  visible.value = false;
}

async function doUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    await uploadImageApi(options.file, {
      scene: props.uploadScene || sceneFilter.value || 'decorate',
    });
    ElMessage.success('已上传并存入素材中心');
    keyword.value = '';
    pageNum.value = 1;
    await load();
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败');
    throw error;
  } finally {
    uploading.value = false;
  }
}

function formatSize(size?: number) {
  if (!size) {
    return '';
  }
  if (size < 1024 * 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}
</script>

<template>
  <ElDialog
    v-model="visible"
    :title="title"
    append-to-body
    destroy-on-close
    width="860px"
  >
    <div class="picker-toolbar">
      <ElInput
        v-model="keyword"
        class="picker-toolbar__search"
        clearable
        placeholder="搜索素材名称"
        @clear="search"
        @keyup.enter="search"
      />
      <ElButton @click="search">查询</ElButton>
      <ElSelect
        v-model="mediaTypeFilter"
        class="picker-toolbar__select"
        placeholder="全部类型"
        clearable
        @change="search"
      >
        <ElOption label="图片" value="image" />
        <ElOption label="视频" value="video" />
        <ElOption label="音频" value="audio" />
      </ElSelect>
      <div class="picker-toolbar__right">
        <ElUpload
          :http-request="doUpload"
          :show-file-list="false"
          accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
        >
          <ElButton :loading="uploading" type="primary">上传新素材</ElButton>
        </ElUpload>
      </div>
    </div>

    <ElEmpty
      v-if="!loading && records.length === 0"
      description="素材中心还没有素材，点右上角上传即可同时入库"
    />

    <div v-loading="loading" class="picker-grid">
      <div
        v-for="item in records"
        :key="item.id"
        class="picker-cell"
        :class="{ picked: isPicked(item.url) }"
        :title="item.name"
        @click="onCellClick(item)"
      >
        <img :src="item.url" :alt="item.name" loading="lazy" />
        <span class="picker-cell__name">{{ item.name }}</span>
        <span class="picker-cell__meta">{{ formatSize(item.size) }}</span>
        <button
          class="picker-cell__zoom"
          title="查看大图"
          type="button"
          @click.stop="viewerUrl = item.url"
        >
          <IconifyIcon icon="lucide:maximize-2" />
        </button>
        <span v-if="isPicked(item.url)" class="picker-cell__check">
          <IconifyIcon icon="lucide:check" />
        </span>
      </div>
    </div>

    <div class="picker-footer">
      <span v-if="multiple" class="picker-footer__count">
        已选 {{ pickedCount }} 张{{ limit > 0 ? ` / 最多 ${limit} 张` : '' }}
      </span>
      <span v-else class="picker-footer__count">点击图片即可选用</span>
      <div class="picker-footer__pager">
        <ElPagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          size="small"
          @current-change="load"
        />
      </div>
    </div>

    <template v-if="multiple" #footer>
      <ElButton @click="visible = false">取消</ElButton>
      <ElButton type="primary" @click="onConfirm">确定选择</ElButton>
    </template>

    <ElImageViewer
      v-if="viewerUrl"
      :url-list="[viewerUrl]"
      teleported
      @close="viewerUrl = ''"
    />
  </ElDialog>
</template>

<style scoped>
.picker-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.picker-toolbar__search {
  width: 220px;
}

.picker-toolbar__select {
  width: 120px;
}

.picker-toolbar__right {
  margin-left: auto;
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  min-height: 180px;
}

.picker-cell {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  border-radius: 6px;
  transition: border-color 0.15s;
}

.picker-cell:hover {
  border-color: var(--el-color-primary);
}

.picker-cell.picked {
  border-color: var(--el-color-primary);
}

.picker-cell img {
  display: block;
  width: 100%;
  height: 104px;
  object-fit: cover;
  background: #f5f7fa;
}

.picker-cell__name {
  display: block;
  padding: 4px 4px 0;
  overflow: hidden;
  font-size: 11px;
  color: #606266;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picker-cell__meta {
  display: block;
  padding: 0 4px 4px;
  font-size: 10px;
  color: #a8abb2;
}

.picker-cell__zoom {
  position: absolute;
  top: 4px;
  right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  color: #fff;
  cursor: pointer;
  background: rgb(0 0 0 / 45%);
  border: none;
  border-radius: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}

.picker-cell:hover .picker-cell__zoom {
  opacity: 1;
}

.picker-cell__check {
  position: absolute;
  top: 4px;
  left: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 12px;
  color: #fff;
  background: var(--el-color-primary);
  border-radius: 50%;
}

.picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.picker-footer__count {
  font-size: 12px;
  color: #909399;
}
</style>
