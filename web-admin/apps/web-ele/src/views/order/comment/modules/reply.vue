<script lang="ts" setup>
import type { OrderCommentApi } from '#/api/order/comment';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { replyComment } from '#/api/order/comment';
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
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'score',
      label: $t('order.comment.score'),
    },
    {
      component: 'Textarea',
      componentProps: { disabled: true, rows: 3 },
      fieldName: 'content',
      label: $t('order.comment.content'),
    },
    {
      component: 'Textarea',
      componentProps: {
        maxLength: 500,
        placeholder: $t('order.comment.replyPlaceholder'),
        rows: 4,
        showCount: true,
      },
      fieldName: 'reply',
      label: $t('order.comment.reply'),
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const summary = computed(() => {
  const comment = current.value;
  if (!comment) {
    return '';
  }
  return `${comment.userNickname ?? $t('order.order.buyerEmpty')} · ${formatTime(comment.commentedAt)}`;
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
      await replyComment(current.value.id, values.reply);
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
        reply: data.reply,
        score: `${'★'.repeat(data.score)}${'☆'.repeat(5 - data.score)}`,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="$t('order.comment.replyTitle')" class="w-[520px]">
    <div class="mb-3 text-sm text-gray-500">{{ summary }}</div>
    <Form />
  </Drawer>
</template>
