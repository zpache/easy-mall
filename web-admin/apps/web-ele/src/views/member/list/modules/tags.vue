<script lang="ts" setup>
import type { MemberApi } from '#/api/member/member';
import type { MemberTagApi } from '#/api/member/tag';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElSelect } from 'element-plus';

import { updateMemberTags } from '#/api/member/member';
import { getTagOptions } from '#/api/member/tag';
import { $t } from '#/locales';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | MemberApi.Member>(null);
const options = ref<MemberTagApi.Tag[]>([]);
const selected = ref<number[]>([]);

async function loadOptions() {
  options.value = await getTagOptions();
}

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    if (!current.value) {
      return;
    }
    drawerApi.lock();
    try {
      await updateMemberTags(current.value.id, selected.value);
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
    selected.value = current.value.tags?.map((tag) => tag.id) ?? [];
    loadOptions();
  },
});
</script>

<template>
  <Drawer :title="$t('member.tag.setTitle')" class="w-[440px]">
    <div class="flex flex-col gap-3 px-4">
      <div class="text-muted-foreground text-sm">
        {{ current?.nickname }}
      </div>
      <ElSelect
        v-model="selected"
        :multiple="true"
        :placeholder="$t('member.tag.selectPlaceholder')"
        class="w-full"
      >
        <ElSelect.Option
          v-for="tag in options"
          :key="tag.id"
          :label="tag.name"
          :value="tag.id"
        >
          {{ tag.name }}（{{ tag.memberCount ?? 0 }}）
        </ElSelect.Option>
      </ElSelect>
      <div class="text-muted-foreground text-xs">
        {{ $t('member.tag.coverTip') }}
      </div>
    </div>
  </Drawer>
</template>
