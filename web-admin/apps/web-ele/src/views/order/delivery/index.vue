<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OrderApi } from '#/api/order/order';

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
  batchDeliverOrders,
  downloadBlob,
  downloadDeliverTemplate,
  getOrderList,
} from '#/api/order/order';
import { $t } from '#/locales';

import Detail from '../list/modules/detail.vue';
import { useColumns, useGridFormSchema } from './data';
import Ship from './modules/ship.vue';

const [ShipDrawer, shipDrawerApi] = useVbenDrawer({
  connectedComponent: Ship,
  destroyOnClose: true,
});

const [DetailDrawer, detailDrawerApi] = useVbenDrawer({
  connectedComponent: Detail,
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
          const range = formValues?.dateRange;
          return await getOrderList({
            endDate: Array.isArray(range) ? range[1] : undefined,
            keyword: formValues?.keyword,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            refundStatus: formValues?.refundStatus,
            startDate: Array.isArray(range) ? range[0] : undefined,
            status: 1,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<OrderApi.Order>,
});

/* ---------------- 批量发货 ---------------- */

const batchVisible = ref(false);
const batchSubmitting = ref(false);
const fileList = ref<UploadUserFile[]>([]);
/** 上传结果：undefined 未提交过 */
const batchResult = ref<OrderApi.BatchDeliverResult | undefined>();

/** el-upload 的 file 只有引用没有内容，取出原始 File 保存 */
let rawFile: File | undefined;

function onFileChange(uploadFile: UploadFile) {
  rawFile = uploadFile.raw instanceof File ? uploadFile.raw : undefined;
}

function onRemove() {
  rawFile = undefined;
}

function onTemplate() {
  downloadDeliverTemplate().then((blob) =>
    downloadBlob(blob, $t('order.delivery.templateName')),
  );
}

async function onSubmitBatch() {
  if (!rawFile) {
    ElMessage.warning($t('order.delivery.pickFileFirst'));
    return;
  }
  batchSubmitting.value = true;
  try {
    batchResult.value = await batchDeliverOrders(rawFile);
    if (batchResult.value.successCount > 0) {
      ElMessage.success(
        $t('order.delivery.batchDone', [batchResult.value.successCount]),
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

function onActionClick(e: OnActionClickParams<OrderApi.Order>) {
  switch (e.code) {
    case 'deliver': {
      shipDrawerApi.setData(e.row).open();
      break;
    }
    case 'detail': {
      detailDrawerApi.setData(e.row).open();
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
    <ShipDrawer @success="refreshGrid" />
    <DetailDrawer />

    <ElDialog
      v-model="batchVisible"
      :title="$t('order.delivery.batchDeliver')"
      width="560px"
    >
      <div class="mb-3 flex items-center gap-2">
        <ElButton link type="primary" @click="onTemplate">
          {{ $t('order.delivery.downloadTemplate') }}
        </ElButton>
        <span class="text-xs text-gray-400">
          {{ $t('order.delivery.batchTip') }}
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
        <div class="py-4">{{ $t('order.delivery.dropHere') }}</div>
      </ElUpload>

      <template v-if="batchResult">
        <ElAlert
          class="mt-3"
          :closable="false"
          :type="batchResult.failures.length > 0 ? 'warning' : 'success'"
          show-icon
          :title="
            $t('order.delivery.batchSummary', [
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
            :key="fail.orderNo"
            class="flex justify-between gap-3 py-0.5"
          >
            <span class="text-gray-600">{{ fail.orderNo }}</span>
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
          {{ $t('order.delivery.startDeliver') }}
        </ElButton>
      </template>
    </ElDialog>

    <Grid :table-title="$t('order.delivery.list')">
      <template #toolbar-tools>
        <ElButton class="mr-2" type="primary" plain @click="openBatch">
          {{ $t('order.delivery.batchDeliver') }}
        </ElButton>
        <ElTag class="mr-2" effect="plain" type="warning">
          {{ $t('order.delivery.tip') }}
        </ElTag>
      </template>
    </Grid>
  </Page>
</template>
