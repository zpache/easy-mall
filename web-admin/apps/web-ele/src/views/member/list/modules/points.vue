<script lang="ts" setup>
import type { MemberApi } from '#/api/member/member';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElOption, ElSelect } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { adjustMemberPoints, getMemberList } from '#/api/member/member';
import { $t } from '#/locales';

const emit = defineEmits<{ success: [] }>();

/** 定向调整时传入的会员；为空则进入通用模式（可搜索选择会员） */
const current = ref<null | MemberApi.Member>(null);
const searchLoading = ref(false);
const memberOptions = ref<MemberApi.Member[]>([]);
const selectedUserId = ref<number>();

const isTargeted = computed(() => !!current.value);

async function searchMembers(query: string) {
  searchLoading.value = true;
  try {
    const result = await getMemberList({ keyword: query || undefined, pageSize: 20 });
    memberOptions.value = result.items;
  } finally {
    searchLoading.value = false;
  }
}

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full' },
      fieldName: 'change',
      help: $t('member.points.changeTip'),
      label: $t('member.points.change'),
      rules: 'required',
    },
    {
      component: 'Textarea',
      componentProps: { maxLength: 255, rows: 3, showCount: true },
      fieldName: 'remark',
      label: $t('member.points.remark'),
    },
  ],
  showDefaultActions: false,
});

const title = computed(() =>
  isTargeted.value
    ? $t('member.points.adjustTitle')
    : $t('member.points.adjustAny'),
);

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const userId = isTargeted.value ? current.value?.id : selectedUserId.value;
    if (!userId) {
      return;
    }
    const values = await formApi.getValues();
    drawerApi.lock();
    try {
      await adjustMemberPoints(userId, {
        change: Number(values.change),
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
    current.value = drawerApi.getData<MemberApi.Member>();
    selectedUserId.value = undefined;
    memberOptions.value = [];
    formApi.resetForm();
    if (!isTargeted.value) {
      searchMembers('');
    }
  },
});
</script>

<template>
  <Drawer :title="title" class="w-[440px]">
    <div class="flex flex-col gap-4 px-4">
      <div v-if="isTargeted" class="text-muted-foreground text-sm">
        {{ current?.nickname }}
      </div>
      <ElSelect
        v-else
        v-model="selectedUserId"
        :filterable="true"
        :loading="searchLoading"
        :placeholder="$t('member.points.selectMember')"
        :remote="true"
        :remote-method="searchMembers"
        class="w-full"
      >
        <ElOption
          v-for="member in memberOptions"
          :key="member.id"
          :label="`${member.nickname}${member.phone ? ' / ' + member.phone : ''}`"
          :value="member.id"
        />
      </ElSelect>
      <Form />
    </div>
  </Drawer>
</template>
