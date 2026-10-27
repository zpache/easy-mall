<script lang="ts" setup>
import type { OrderRefundApi } from '#/api/order/refund';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { auditRefund } from '#/api/order/refund';
import { $t } from '#/locales';

import { formatMoney, formatTime } from '../../list/data';
import { typeLabel } from '../data';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | OrderRefundApi.Refund>(null);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'refundNo',
      label: $t('order.refund.refundNo'),
    },
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'amount',
      label: $t('order.refund.amount'),
    },
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'orderNo',
      label: $t('order.refund.orderNo'),
    },
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'type',
      label: $t('order.refund.type'),
    },
    {
      component: 'Textarea',
      componentProps: { disabled: true, rows: 2 },
      fieldName: 'reason',
      label: $t('order.refund.reason'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('order.refund.passYes'), value: true },
          { label: $t('order.refund.passNo'), value: false },
        ],
      },
      defaultValue: true,
      fieldName: 'pass',
      label: $t('order.refund.pass'),
      rules: 'selectRequired',
    },
    {
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        placeholder: $t('order.refund.remarkPlaceholder'),
        rows: 3,
        showCount: true,
      },
      fieldName: 'remark',
      label: $t('order.refund.remark'),
    },
  ],
  showDefaultActions: false,
});

const summary = computed(() => {
  const refund = current.value;
  if (!refund) {
    return '';
  }
  return `${typeLabel(refund.type)} · ${$t('order.refund.appliedAt')} ${formatTime(refund.appliedAt)}`;
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid || !current.value) {
      return;
    }
    const values = await formApi.getValues();
    drawerApi.lock();
    try {
      await auditRefund(current.value.id, {
        pass: Boolean(values.pass),
        remark: values.remark,
      });
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<OrderRefundApi.Refund>();
    current.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        amount: formatMoney(data.amount),
        orderNo: data.orderNo,
        pass: true,
        reason: data.reason,
        refundNo: data.refundNo,
        type: typeLabel(data.type),
      });
    }
  },
});
</script>

<template>
  <Drawer :title="$t('order.refund.auditTitle')" class="w-[520px]">
    <div class="mb-3 text-sm text-gray-500">{{ summary }}</div>
    <Form />
  </Drawer>
</template>
