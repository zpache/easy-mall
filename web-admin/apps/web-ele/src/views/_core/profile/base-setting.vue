<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { ElDescriptions, ElDescriptionsItem, ElTag } from 'element-plus';

import { getUserInfoApi } from '#/api';

/** /user/info 返回结构（展示所需的字段） */
interface ProfileInfo {
  avatar?: string;
  homePath?: string;
  realName?: string;
  roleNames?: string[];
  roles?: string[];
  userId?: number | string;
  username?: string;
}

const info = ref<ProfileInfo | null>(null);

onMounted(async () => {
  info.value = await getUserInfoApi();
});
</script>

<template>
  <ElDescriptions
    v-if="info"
    :column="1"
    border
    class="max-w-[560px]"
    size="small"
  >
    <ElDescriptionsItem label="用户 ID">
      {{ info.userId ?? '-' }}
    </ElDescriptionsItem>
    <ElDescriptionsItem label="登录账号">
      {{ info.username || '-' }}
    </ElDescriptionsItem>
    <ElDescriptionsItem label="姓名">
      {{ info.realName || '-' }}
    </ElDescriptionsItem>
    <ElDescriptionsItem label="角色">
      <template v-if="info.roleNames?.length">
        <ElTag
          v-for="name in info.roleNames"
          :key="name"
          class="mr-1"
          effect="plain"
        >
          {{ name }}
        </ElTag>
      </template>
      <span v-else>-</span>
    </ElDescriptionsItem>
    <ElDescriptionsItem label="默认首页">
      {{ info.homePath || '-' }}
    </ElDescriptionsItem>
  </ElDescriptions>
</template>
