<script lang="ts" setup>
import type { OrderApi } from '#/api/order/order';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { deliverOrder } from '#/api/order/order';
import { $t } from '#/locales';

import { formatMoney, formatTime, summaryGoods } from '../../list/data';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | OrderApi.Order>(null);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'orderNo',
      label: $t('order.order.orderNo'),
    },
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'goods',
      label: $t('order.order.goods'),
    },
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'receiver',
      label: $t('order.order.receiverInfo'),
    },
    {
      component: 'Select',
      componentProps: {
        allowCreate: true,
        filterable: true,
        options: [
          { label: '顺丰速运', value: '顺丰速运' },
          { label: '京东物流', value: '京东物流' },
          { label: '中通快递', value: '中通快递' },
          { label: '圆通速递', value: '圆通速递' },
          { label: '申通快递', value: '申通快递' },
          { label: '韵达快递', value: '韵达快递' },
          { label: '邮政 EMS', value: '邮政 EMS' },
          { label: '极兔速递', value: '极兔速递' },
        ],
        placeholder: $t('order.delivery.companyPlaceholder'),
      },
      fieldName: 'deliveryCompany',
      label: $t('order.delivery.company'),
      rules: 'selectRequired',
    },
    {
      component: 'Input',
      componentProps: {
        placeholder: $t('order.delivery.noPlaceholder'),
      },
      fieldName: 'deliveryNo',
      label: $t('order.delivery.no'),
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const orderInfo = computed(() => {
  const order = current.value;
  if (!order) {
    return '';
  }
  return `${formatMoney(order.payAmount)} · ${formatTime(order.createdAt)}`;
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
      await deliverOrder(current.value.id, {
        deliveryCompany: values.deliveryCompany,
        deliveryNo: values.deliveryNo,
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
    const data = drawerApi.getData<OrderApi.Order>();
    current.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        deliveryCompany: data.deliveryCompany,
        deliveryNo: data.deliveryNo,
        goods: summaryGoods(data),
        orderNo: data.orderNo,
        receiver: `${data.receiverName} ${data.receiverPhone} · ${data.receiverAddress}`,
      });
    }
  },
});
</script>

<template>
  <Drawer
    :title="`${$t('order.delivery.shipTitle')} · ${current?.orderNo ?? ''}`"
    class="w-[520px]"
  >
    <div class="mb-3 text-sm text-gray-500">{{ orderInfo }}</div>
    <Form />
  </Drawer>
</template>
