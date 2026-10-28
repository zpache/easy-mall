<script lang="ts" setup>
import { useVbenForm } from '#/adapter/form';
import { useVbenModal } from '@vben/common-ui';

import { useFormSchema } from '../data';

const emit = defineEmits<{ submit: [values: any] }>();

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    emit('submit', values);
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      formApi.resetForm();
    }
  },
});

function close() {
  modalApi.close();
}

defineExpose({ close });
</script>

<template>
  <Modal title="新建装修页面" class="w-[460px]">
    <Form />
  </Modal>
</template>
