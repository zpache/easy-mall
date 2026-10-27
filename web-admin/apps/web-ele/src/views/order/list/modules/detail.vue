<script lang="ts" setup>
import type { OrderApi } from '#/api/order/order';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElDescriptions,
  ElDescriptionsItem,
  ElImage,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { getOrderDetail } from '#/api/order/order';
import { $t } from '#/locales';

import {
  formatMoney,
  formatTime,
  useRefundStatusOptions,
  useStatusOptions,
} from '../data';

const order = ref<null | OrderApi.Order>(null);
const loading = ref(false);

const [Drawer, drawerApi] = useVbenDrawer({
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<OrderApi.Order>();
    if (!data?.id) {
      order.value = null;
      return;
    }
    loading.value = true;
    try {
      order.value = await getOrderDetail(data.id);
    } finally {
      loading.value = false;
    }
  },
});

function statusOption(value?: number) {
  return useStatusOptions().find((item) => item.value === value);
}

function refundOption(value?: number) {
  return useRefundStatusOptions().find((item) => item.value === value);
}

const tagTypeMap: Record<string, any> = {
  default: 'info',
  error: 'danger',
  processing: 'primary',
  success: 'success',
  warning: 'warning',
};
</script>

<template>
  <Drawer
    :title="`${$t('order.order.detailTitle')} · ${order?.orderNo ?? ''}`"
    class="w-[760px]"
  >
    <div v-loading="loading" class="space-y-5">
      <template v-if="order">
        <ElDescriptions
          :column="2"
          :title="$t('order.order.baseInfo')"
          border
          size="small"
        >
          <ElDescriptionsItem :label="$t('order.order.orderNo')">
            {{ order.orderNo }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.status')">
            <ElTag
              :type="tagTypeMap[statusOption(order.status)?.color ?? 'default']"
              effect="plain"
            >
              {{ statusOption(order.status)?.label }}
            </ElTag>
            <ElTag
              v-if="order.refundStatus !== 0"
              class="ml-1"
              :type="
                tagTypeMap[refundOption(order.refundStatus)?.color ?? 'default']
              "
              effect="plain"
            >
              {{ refundOption(order.refundStatus)?.label }}
            </ElTag>
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.buyer')">
            {{ order.userNickname || $t('order.order.buyerEmpty') }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.createdAt')">
            {{ formatTime(order.createdAt) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.payTime')">
            {{ formatTime(order.payTime) || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.deliveryTime')">
            {{ formatTime(order.deliveryTime) || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.finishTime')">
            {{ formatTime(order.finishTime) || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem
            v-if="order.status === 4"
            :label="$t('order.order.cancelTime')"
          >
            {{ formatTime(order.cancelTime) || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem
            v-if="order.remark"
            :label="$t('order.order.remark')"
            :span="2"
          >
            {{ order.remark }}
          </ElDescriptionsItem>
          <ElDescriptionsItem
            v-if="order.cancelReason"
            :label="$t('order.order.cancelReason')"
            :span="2"
          >
            {{ order.cancelReason }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <ElDescriptions
          :column="2"
          :title="$t('order.order.receiverInfo')"
          border
          size="small"
        >
          <ElDescriptionsItem :label="$t('order.order.receiver')">
            {{ order.receiverName }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.receiverPhone')">
            {{ order.receiverPhone }}
          </ElDescriptionsItem>
          <ElDescriptionsItem
            :label="$t('order.order.receiverAddress')"
            :span="2"
          >
            {{ order.receiverAddress }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <ElDescriptions
          :column="2"
          :title="$t('order.order.deliveryInfo')"
          border
          size="small"
        >
          <ElDescriptionsItem :label="$t('order.order.deliveryCompany')">
            {{ order.deliveryCompany || '-' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem :label="$t('order.order.deliveryNo')">
            {{ order.deliveryNo || '-' }}
          </ElDescriptionsItem>
        </ElDescriptions>

        <div>
          <div class="mb-2 text-sm font-medium">
            {{ $t('order.order.items') }}
          </div>
          <ElTable :data="order.items" border size="small">
            <ElTableColumn :label="$t('order.order.goods')" min-width="240">
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <ElImage
                    :preview-src-list="row.goodsImage ? [row.goodsImage] : []"
                    :src="row.goodsImage"
                    fit="cover"
                    style="width: 40px; height: 40px; flex-shrink: 0"
                  />
                  <span>{{ row.goodsName }}</span>
                </div>
              </template>
            </ElTableColumn>
            <ElTableColumn :label="$t('order.order.price')" width="100">
              <template #default="{ row }">
                {{ formatMoney(row.price) }}
              </template>
            </ElTableColumn>
            <ElTableColumn
              :label="$t('order.order.quantity')"
              prop="quantity"
              width="80"
            />
            <ElTableColumn :label="$t('order.order.subtotal')" width="110">
              <template #default="{ row }">
                {{ formatMoney(row.totalAmount) }}
              </template>
            </ElTableColumn>
          </ElTable>
        </div>

        <div class="flex justify-end gap-6 text-sm">
          <span>
            {{ $t('order.order.totalAmount') }}：{{
              formatMoney(order.totalAmount)
            }}
          </span>
          <span>
            {{ $t('order.order.freight') }}：{{
              formatMoney(order.freightAmount)
            }}
          </span>
          <span class="font-medium text-red-500">
            {{ $t('order.order.amount') }}：{{ formatMoney(order.payAmount) }}
          </span>
        </div>
      </template>
    </div>
  </Drawer>
</template>
