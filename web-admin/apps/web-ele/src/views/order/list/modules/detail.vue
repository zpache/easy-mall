<script lang="ts" setup>
import type { OrderApi } from '#/api/order/order';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElAlert,
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElImage,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus';

import { getOrderDetail, getOrderLogistics } from '#/api/order/order';
import { $t } from '#/locales';

import {
  formatMoney,
  formatTime,
  useRefundStatusOptions,
  useStatusOptions,
} from '../data';

const order = ref<null | OrderApi.Order>(null);
const loading = ref(false);

/** 物流轨迹：undefined 未加载，null 加载失败/不可用 */
const trace = ref<OrderApi.LogisticsTrace | null | undefined>(undefined);
const traceError = ref('');
const traceLoading = ref(false);

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
      trace.value = undefined;
      traceError.value = '';
      // 已发货且有单号时自动加载物流轨迹（未开启/失败静默降级，不阻塞详情展示）
      if (order.value?.deliveryNo) {
        loadTrace(order.value.id);
      }
    } finally {
      loading.value = false;
    }
  },
});

async function loadTrace(id: number) {
  traceLoading.value = true;
  traceError.value = '';
  try {
    trace.value = await getOrderLogistics(id);
  } catch (error: any) {
    trace.value = null;
    traceError.value = error?.message || $t('order.order.logisticsFailed');
  } finally {
    traceLoading.value = false;
  }
}

function statusOption(value?: number) {
  return useStatusOptions().find((item) => item.value === value);
}

function refundOption(value?: number) {
  return useRefundStatusOptions().find((item) => item.value === value);
}

/** 支付方式文案；null 为存量已支付订单（未记录渠道），显示未知 */
function payTypeLabel(value?: null | number) {
  if (value === null || value === undefined) {
    return formatTime(order.value?.payTime) ? $t('order.order.payTypeUnknown') : '-';
  }
  const labels: Record<number, string> = {
    0: $t('order.order.payTypeMock'),
    1: $t('order.order.payTypeWxpay'),
    2: $t('order.order.payTypeAlipay'),
  };
  return labels[value] ?? '-';
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
          <ElDescriptionsItem :label="$t('order.order.payType')">
            {{ payTypeLabel(order.payType) }}
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

        <div v-if="order.deliveryNo">
          <div class="mb-2 flex items-center justify-between">
            <span class="text-sm font-medium">
              {{ $t('order.order.logisticsTrace') }}
            </span>
            <ElButton
              :loading="traceLoading"
              link
              type="primary"
              @click="loadTrace(order.id)"
            >
              {{ $t('order.order.logisticsRefresh') }}
            </ElButton>
          </div>
          <ElAlert
            v-if="trace === null"
            :title="traceError"
            :closable="false"
            show-icon
            type="warning"
          />
          <ElTimeline v-else-if="trace?.traces?.length" class="pl-2">
            <ElTimelineItem
              v-for="(node, index) in trace.traces"
              :key="`${node.time}-${index}`"
              :hollow="index !== 0"
              :timestamp="node.ftime || node.time"
              :type="index === 0 ? 'primary' : undefined"
              placement="top"
            >
              <div>{{ node.context }}</div>
              <div
                v-if="node.location"
                class="text-xs text-gray-400"
              >
                {{ node.location }}
              </div>
            </ElTimelineItem>
          </ElTimeline>
          <div v-else-if="trace" class="text-sm text-gray-400">
            {{ $t('order.order.logisticsEmpty') }}
          </div>
        </div>

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
          <span v-if="order.memberDiscount > 0">
            {{ $t('order.order.memberDiscount')
            }}{{ order.memberLevelName ? `（${order.memberLevelName}）` : ''
            }}：-{{ formatMoney(order.memberDiscount) }}
          </span>
          <span v-if="order.reductionAmount > 0">
            {{ $t('order.order.reduction') }}：-{{ formatMoney(order.reductionAmount) }}
          </span>
          <span v-if="order.couponAmount > 0">
            {{ $t('order.order.coupon') }}：-{{ formatMoney(order.couponAmount) }}
          </span>
          <span>
            {{ $t('order.order.freight') }}：{{
              formatMoney(order.freightAmount)
            }}
          </span>
          <span class="font-medium text-red-500">
            {{ $t('order.order.amount') }}：{{ formatMoney(order.payAmount) }}
          </span>
          <span v-if="order.refundedAmount > 0" class="text-orange-500">
            {{ $t('order.order.refundedAmount') }}：{{
              formatMoney(order.refundedAmount)
            }}
          </span>
        </div>
      </template>
    </div>
  </Drawer>
</template>
