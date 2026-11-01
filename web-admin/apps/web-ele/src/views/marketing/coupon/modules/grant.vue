<script lang="ts" setup>
import type { MarketingCouponApi } from '#/api/marketing/coupon';
import type { MemberApi } from '#/api/member/member';

import { computed, reactive, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  ElButton,
  ElInput,
  ElMessage,
  ElPagination,
  ElSelect,
  ElOption,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { grantCoupon } from '#/api/marketing/coupon';
import { getMemberList, getTagOptions } from '#/api/member';

const emit = defineEmits<{ success: [] }>();

const current = ref<null | MarketingCouponApi.Coupon>(null);
const submitting = ref(false);

/** 标签筛选 + 会员分页数据 */
const tags = ref<MemberApi.TagItem[]>([]);
const tagId = ref<number | undefined>();
const keyword = ref('');
const members = ref<MemberApi.Member[]>([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
const loading = ref(false);

const selectedIds = ref<number[]>([]);

const couponLabel = computed(() =>
  current.value
    ? `${current.value.name}（已发 ${current.value.sendCount ?? 0}/${
        current.value.totalCount && current.value.totalCount > 0
          ? current.value.totalCount
          : '不限'
      }，限领 ${current.value.perLimit ?? 1} 张/人）`
    : '',
);

const [Modal, modalApi] = useVbenModal({
  class: 'w-[720px]',
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    current.value = modalApi.getData<MarketingCouponApi.Coupon>();
    selectedIds.value = [];
    tagId.value = undefined;
    keyword.value = '';
    page.pageNum = 1;
    if (tags.value.length === 0) {
      try {
        tags.value = (await getTagOptions()) ?? [];
      } catch {
        tags.value = [];
      }
    }
    loadMembers();
  },
});

async function loadMembers() {
  loading.value = true;
  try {
    const result = await getMemberList({
      keyword: keyword.value || undefined,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      tagId: tagId.value,
    });
    members.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

watch([tagId], () => {
  page.pageNum = 1;
  loadMembers();
});

function onSearch() {
  page.pageNum = 1;
  loadMembers();
}

function onPageChange() {
  loadMembers();
}

function onSelectAll() {
  // 勾选当前页全部
  for (const member of members.value) {
    if (!selectedIds.value.includes(member.id)) {
      selectedIds.value.push(member.id);
    }
  }
}

function onClearSelection() {
  selectedIds.value = [];
}

function isRowSelected(row: MemberApi.Member) {
  return selectedIds.value.includes(row.id);
}

function toggleRow(row: MemberApi.Member) {
  const index = selectedIds.value.indexOf(row.id);
  if (index >= 0) {
    selectedIds.value.splice(index, 1);
  } else {
    selectedIds.value.push(row.id);
  }
}

async function onSubmit() {
  if (!current.value) {
    return;
  }
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选要发放的会员');
    return;
  }
  submitting.value = true;
  try {
    const granted = await grantCoupon(current.value.id, selectedIds.value);
    ElMessage.success(`已发放 ${granted} 张`);
    modalApi.close();
    emit('success');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Modal title="优惠券定向发放">
    <div v-if="current" class="space-y-3">
      <ElTag effect="plain" type="primary">{{ couponLabel }}</ElTag>

      <div class="flex flex-wrap items-center gap-2">
        <ElSelect
          v-model="tagId"
          class="w-52"
          clearable
          placeholder="按会员标签筛选（可选）"
          size="small"
        >
          <ElOption
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          />
        </ElSelect>
        <ElInput
          v-model="keyword"
          class="w-48"
          clearable
          placeholder="昵称/手机号搜索"
          size="small"
          @keyup.enter="onSearch"
        />
        <ElButton size="small" @click="onSearch">搜索</ElButton>
        <div class="ml-auto flex gap-2">
          <ElButton size="small" @click="onSelectAll">勾选本页全部</ElButton>
          <ElButton size="small" @click="onClearSelection">清空勾选</ElButton>
        </div>
      </div>

      <ElTable
        v-loading="loading"
        :data="members"
        height="320px"
        size="small"
        @row-click="toggleRow"
      >
        <ElTableColumn width="44">
          <template #default="{ row }">
            <input
              :checked="isRowSelected(row)"
              type="checkbox"
              @click.stop="toggleRow(row)"
            />
          </template>
        </ElTableColumn>
        <ElTableColumn label="会员">
          <template #default="{ row }">
            {{ row.nickname }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="手机号" width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="标签" min-width="140">
          <template #default="{ row }">
            <ElTag
              v-for="tag in row.tags ?? []"
              :key="tag.id"
              class="mr-1"
              effect="plain"
              size="small"
            >
              {{ tag.name }}
            </ElTag>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="flex items-center justify-between">
        <span class="text-xs text-gray-500">
          已勾选 {{ selectedIds.length }} 位会员
        </span>
        <ElPagination
          v-model:current-page="page.pageNum"
          :page-size="page.pageSize"
          :total="total"
          layout="prev, pager, next"
          size="small"
          @current-change="onPageChange"
        />
      </div>

      <div class="flex justify-end gap-2 pt-1">
        <ElButton @click="modalApi.close()">取消</ElButton>
        <ElButton
          :disabled="selectedIds.length === 0"
          :loading="submitting"
          type="primary"
          @click="onSubmit"
        >
          发放给已勾选会员
        </ElButton>
      </div>
    </div>
  </Modal>
</template>
