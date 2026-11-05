<script lang="ts" setup>
import type { OrderApi } from '#/api/order/order';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { deliverOrder } from '#/api/order/order';
import { getDeliveryCompanies } from '#/api/settings/delivery';
import { $t } from '#/locales';

import { formatMoney, formatTime, summaryGoods } from '../../list/data';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | OrderApi.Order>(null);

/** 发货下拉的快递公司来自「商城设置 → 快递公司」，只取启用的 */
const companyOptions = ref<{ label: string; value: string }[]>([]);

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
        options: [] as { label: string; value: string }[],
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

/** 拉取启用的快递公司；失败时只提示，不阻塞发货（仍可手填） */
async function loadCompanyOptions() {
  try {
    const list = await getDeliveryCompanies({ status: 1 });
    companyOptions.value = list.map((item) => ({
      label: item.name,
      value: item.name,
    }));
  } catch {
    companyOptions.value = [];
  }
  formApi.updateSchema([
    {
      componentProps: { options: companyOptions.value },
      fieldName: 'deliveryCompany',
    },
  ]);
}

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
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<OrderApi.Order>();
    current.value = data && data.id ? data : null;
    formApi.resetForm();
    // 先拉快递公司再写入回显值：updateSchema 会重建字段，放在 setValues 之后会把值清掉
    await loadCompanyOptions();
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
