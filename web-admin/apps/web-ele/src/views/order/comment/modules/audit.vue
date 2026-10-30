<script lang="ts" setup>
import type { OrderCommentApi } from '#/api/order/comment';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { auditComment } from '#/api/order/comment';
import { $t } from '#/locales';

import { formatTime } from '../../list/data';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | OrderCommentApi.Comment>(null);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'goodsName',
      label: $t('order.comment.goods'),
    },
    {
      component: 'Textarea',
      componentProps: { disabled: true, rows: 3 },
      fieldName: 'content',
      label: $t('order.comment.content'),
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('order.comment.auditPass'), value: 1 },
          { label: $t('order.comment.auditReject'), value: 2 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: $t('order.comment.auditResult'),
      rules: 'selectRequired',
    },
    {
      component: 'Textarea',
      componentProps: {
        maxLength: 200,
        placeholder: $t('order.comment.auditRemarkPlaceholder'),
        rows: 3,
        showCount: true,
      },
      fieldName: 'remark',
      label: $t('order.comment.auditRemark'),
    },
  ],
  showDefaultActions: false,
});

const summary = computed(() => {
  const comment = current.value;
  if (!comment) {
    return '';
  }
  const who = comment.userNickname || $t('order.order.buyerEmpty');
  const stars = `${'★'.repeat(comment.score)}${'☆'.repeat(5 - comment.score)}`;
  return `${who} · ${stars} · ${formatTime(comment.commentedAt)}`;
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid || !current.value) {
      return;
    }
    const values = await formApi.getValues();
    // 拒绝必须说明原因，用户在小程序端能看到这条备注
    if (values.status === 2 && !String(values.remark ?? '').trim()) {
      ElMessage.warning($t('order.comment.auditRejectRemarkRequired'));
      return;
    }
    drawerApi.lock();
    try {
      await auditComment(
        current.value.id,
        values.status as OrderCommentApi.AuditStatus,
        values.remark,
      );
      ElMessage.success($t('order.comment.auditSuccess'));
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
    const data = drawerApi.getData<OrderCommentApi.Comment>();
    current.value = data && data.id ? data : null;
    formApi.resetForm();
    if (data?.id) {
      formApi.setValues({
        content: data.content,
        goodsName: data.goodsName,
        remark: data.auditRemark,
        status: 1,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="$t('order.comment.auditTitle')" class="w-[520px]">
    <div class="mb-3 text-sm text-gray-500">{{ summary }}</div>
    <Form />
  </Drawer>
</template>
