<script lang="ts" setup>
import type { MaterialApi } from '#/api/decorate/material';
import type { UploadRequestOptions } from 'element-plus';

import { ref, watch } from 'vue';

import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElInput,
  ElMessage,
  ElPagination,
  ElUpload,
} from 'element-plus';

import { getMaterials } from '#/api/decorate/material';
import { uploadImageApi } from '#/api/upload';

defineOptions({ name: 'MaterialPicker' });

const props = defineProps<{
  /** 图片点击选中后是否自动关闭（单选场景 true；多选场景 false 自行处理） */
  autoClose?: boolean;
  modelValue: boolean;
  title?: string;
}>();

const emit = defineEmits<{
  select: [url: string];
  'update:modelValue': [value: boolean];
}>();

const visible = ref(props.modelValue);
watch(
  () => props.modelValue,
  (val) => (visible.value = val),
);
watch(visible, (val) => emit('update:modelValue', val));

const loading = ref(false);
const uploading = ref(false);
const records = ref<MaterialApi.Material[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(18);
const keyword = ref('');

async function load() {
  loading.value = true;
  try {
    const result = await getMaterials({
      keyword: keyword.value || undefined,
      mediaType: 'image',
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    records.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

watch(visible, (val) => {
  if (val) {
    pageNum.value = 1;
    load();
  }
});

function pick(item: MaterialApi.Material) {
  emit('select', item.url);
  if (props.autoClose !== false) {
    visible.value = false;
  }
}

async function doUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    const result = await uploadImageApi(options.file, { scene: 'decorate' });
    ElMessage.success('上传成功');
    pageNum.value = 1;
    await load();
    return result.url;
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败');
    throw error;
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <ElDialog
    v-model="visible"
    :title="title || '从素材库选择'"
    destroy-on-close
    width="720px"
  >
    <div class="picker-toolbar">
      <ElInput
        v-model="keyword"
        class="picker-toolbar__search"
        clearable
        placeholder="搜索素材名称"
        @keyup.enter="
          () => {
            pageNum = 1;
            load();
          }
        "
      />
      <ElButton @click="pageNum = 1; load()">查询</ElButton>
      <ElUpload
        :http-request="doUpload"
        :show-file-list="false"
        accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
      >
        <ElButton :loading="uploading" type="primary">上传新图片</ElButton>
      </ElUpload>
    </div>

    <ElEmpty
      v-if="!loading && records.length === 0"
      description="素材库为空，先上传几张图片吧"
    />
    <div v-loading="loading" class="picker-grid">
      <div
        v-for="item in records"
        :key="item.id"
        class="picker-cell"
        :title="item.name"
        @click="pick(item)"
      >
        <img :src="item.url" :alt="item.name" loading="lazy" />
      </div>
    </div>

    <div v-if="total > pageSize" class="picker-pager">
      <ElPagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
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

.picker-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  min-height: 160px;
}

.picker-cell {
  overflow: hidden;
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.picker-cell:hover {
  border-color: var(--el-color-primary);
}

.picker-cell img {
  display: block;
  width: 100%;
  height: 110px;
  object-fit: cover;
}

.picker-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
