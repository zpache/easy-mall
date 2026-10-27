<script lang="ts" setup>
import type {
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getCouponRecords } from '#/api/marketing/coupon';
import { $t } from '#/locales';

import { useRecordColumns } from '../data';

const [Grid] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        component: 'InputNumber',
        componentProps: {
          class: 'w-full',
          controlsPosition: 'right',
          min: 1,
          placeholder: $t('marketing.coupon.recordsCouponIdPlaceholder'),
        },
        fieldName: 'couponId',
        label: $t('marketing.coupon.recordsCouponId'),
      },
      {
        component: 'InputNumber',
        componentProps: {
          class: 'w-full',
          controlsPosition: 'right',
          min: 1,
          placeholder: $t('marketing.coupon.recordsUserIdPlaceholder'),
        },
        fieldName: 'userId',
        label: $t('marketing.coupon.recordUser'),
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: [
            { label: $t('marketing.coupon.recordUnused'), value: 0 },
            { label: $t('marketing.coupon.recordUsed'), value: 1 },
            { label: $t('marketing.coupon.recordExpired'), value: 2 },
          ],
        },
        fieldName: 'status',
        label: $t('marketing.coupon.recordStatus'),
      },
    ],
    submitOnChange: true,
  },
  gridOptions: {
    columns: useRecordColumns(),
    height: 440,
    keepSource: true,
    pagerConfig: {
      pageSize: 10,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getCouponRecords({
            couponId: formValues?.couponId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            status: formValues?.status,
            userId: formValues?.userId,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: false,
      export: false,
      refresh: true,
      search: true,
      zoom: false,
    },
  } as VxeTableGridOptions,
});

const [Drawer] = useVbenDrawer({
  footer: false,
});
</script>

<template>
  <Drawer :title="$t('marketing.coupon.recordsList')" class="w-[1100px]">
    <Grid />
  </Drawer>
</template>
