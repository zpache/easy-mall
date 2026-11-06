<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { GoodsStockApi } from '#/api/goods/stock';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';

import {
  ElAlert,
  ElButton,
  ElDialog,
  ElMessage,
  ElTag,
  ElUpload,
  type UploadFile,
  type UploadUserFile,
} from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  batchSetStock,
  downloadBatchStockTemplate,
  downloadBlob,
  getStockList,
} from '#/api/goods/stock';
import { GOODS_PERMISSIONS } from '#/constants/permissions';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Adjust from './modules/adjust.vue';
import Logs from './modules/logs.vue';

const [AdjustDrawer, adjustDrawerApi] = useVbenDrawer({
  connectedComponent: Adjust,
  destroyOnClose: true,
});

const [LogsDrawer, logsDrawerApi] = useVbenDrawer({
  connectedComponent: Logs,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getStockList({
            keyword: formValues?.keyword,
            lowStock: formValues?.lowStock === true ? true : undefined,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
          });
        },
      },
    },
    rowConfig: { keyField: 'goodsId' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<GoodsStockApi.Stock>,
});

/* ---------------- 批量设置库存 ---------------- */

const batchVisible = ref(false);
const batchSubmitting = ref(false);
const fileList = ref<UploadUserFile[]>([]);
/** 上传结果：undefined 未提交过 */
const batchResult = ref<GoodsStockApi.BatchStockResult | undefined>();

/** el-upload 的 file 只有引用没有内容，取出原始 File 保存 */
let rawFile: File | undefined;

function onFileChange(uploadFile: UploadFile) {
  rawFile = uploadFile.raw instanceof File ? uploadFile.raw : undefined;
}

function onRemove() {
  rawFile = undefined;
}

function onTemplate() {
  downloadBatchStockTemplate().then((blob) =>
    downloadBlob(blob, $t('goods.stock.templateName')),
  );
}

async function onSubmitBatch() {
  if (!rawFile) {
    ElMessage.warning($t('goods.stock.pickFileFirst'));
    return;
  }
  batchSubmitting.value = true;
  try {
    batchResult.value = await batchSetStock(rawFile);
    if (batchResult.value.successCount > 0) {
      ElMessage.success(
        $t('goods.stock.batchDone', [batchResult.value.successCount]),
      );
      gridApi.query();
    }
  } finally {
    batchSubmitting.value = false;
  }
}

function openBatch() {
  fileList.value = [];
  rawFile = undefined;
  batchResult.value = undefined;
  batchVisible.value = true;
}

function onActionClick(e: OnActionClickParams<GoodsStockApi.Stock>) {
  switch (e.code) {
    case 'adjust': {
      adjustDrawerApi.setData(e.row).open();
      break;
    }
    case 'logs': {
      logsDrawerApi.setData(e.row).open();
      break;
    }
  }
}

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <AdjustDrawer @success="refreshGrid" />
    <LogsDrawer />

    <ElDialog
      v-model="batchVisible"
      :title="$t('goods.stock.batchSet')"
      width="560px"
    >
      <div class="mb-3 flex items-center gap-2">
        <ElButton link type="primary" @click="onTemplate">
          {{ $t('goods.stock.downloadTemplate') }}
        </ElButton>
        <span class="text-xs text-gray-400">
          {{ $t('goods.stock.batchTip') }}
        </span>
      </div>
      <ElUpload
        v-model:file-list="fileList"
        :accept="'.xlsx'"
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-remove="onRemove"
        drag
      >
        <div class="py-4">{{ $t('goods.stock.dropHere') }}</div>
      </ElUpload>

      <template v-if="batchResult">
        <ElAlert
          class="mt-3"
          :closable="false"
          :type="batchResult.failures.length > 0 ? 'warning' : 'success'"
          show-icon
          :title="
            $t('goods.stock.batchSummary', [
              batchResult.total,
              batchResult.successCount,
              batchResult.failures.length,
            ])
          "
        />
        <div
          v-if="batchResult.failures.length"
          class="mt-2 max-h-40 overflow-auto text-xs"
        >
          <div
            v-for="fail in batchResult.failures"
            :key="fail.goodsCode"
            class="flex justify-between gap-3 py-0.5"
          >
            <span class="text-gray-600">
              {{ fail.goodsCode }}
              <template v-if="fail.goodsName">（{{ fail.goodsName }}）</template>
            </span>
            <span class="text-red-500">{{ fail.reason }}</span>
          </div>
        </div>
      </template>

      <template #footer>
        <ElButton @click="batchVisible = false">
          {{ $t('common.close') }}
        </ElButton>
        <ElButton
          :disabled="!rawFile"
          :loading="batchSubmitting"
          type="primary"
          @click="onSubmitBatch"
        >
          {{ $t('goods.stock.startSet') }}
        </ElButton>
      </template>
    </ElDialog>

    <Grid :table-title="$t('goods.stock.list')">
      <template #toolbar-tools>
        <ElButton
          v-access:code="GOODS_PERMISSIONS.stockImport"
          class="mr-2"
          type="primary"
          plain
          @click="openBatch"
        >
          {{ $t('goods.stock.batchSet') }}
        </ElButton>
        <ElTag type="warning" effect="plain">
          {{ $t('goods.stock.lowStockTip') }}
        </ElTag>
      </template>
    </Grid>
  </Page>
</template>
