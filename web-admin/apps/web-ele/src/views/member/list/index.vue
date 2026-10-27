<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { MemberApi } from '#/api/member/member';

import { onMounted } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getLevelOptions } from '#/api/member/level';
import { getMemberList } from '#/api/member/member';
import { getTagOptions } from '#/api/member/tag';
import { $t } from '#/locales';

import PointsDrawer from './modules/points.vue';
import TagsDrawer from './modules/tags.vue';
import {
  levelOptionList,
  setLevelOptions,
  setTagOptions,
  tagOptionList,
  useColumns,
  useGridFormSchema,
} from './data';

const [PointsAdjustDrawer, pointsDrawerApi] = useVbenDrawer({
  connectedComponent: PointsDrawer,
  destroyOnClose: true,
});

const [MemberTagsDrawer, tagsDrawerApi] = useVbenDrawer({
  connectedComponent: TagsDrawer,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      // 关闭自动加载：等等级 / 标签下拉选项就绪后再查询，保证标签列渲染正确
      autoLoad: false,
      ajax: {
        query: async ({ page }, formValues) => {
          return await getMemberList({
            keyword: formValues?.keyword,
            levelId: formValues?.levelId,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            source: formValues?.source,
            tagId: formValues?.tagId,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<MemberApi.Member>,
});

function onActionClick(e: OnActionClickParams<MemberApi.Member>) {
  switch (e.code) {
    case 'points': {
      pointsDrawerApi.setData(e.row).open();
      break;
    }
    case 'tags': {
      tagsDrawerApi.setData(e.row).open();
      break;
    }
  }
}

/** 注入等级与标签下拉选项（筛选与表格标签渲染共用），就绪后更新表单并触发首查 */
async function loadOptions() {
  const [levels, tags] = await Promise.all([getLevelOptions(), getTagOptions()]);
  setLevelOptions(
    levels.map((level) => ({ label: level.name, value: level.id })),
  );
  setTagOptions(tags.map((tag) => ({ label: tag.name, value: tag.id })));
  gridApi.formApi?.updateSchema([
    { componentProps: { options: levelOptionList }, fieldName: 'levelId' },
    { componentProps: { options: tagOptionList }, fieldName: 'tagId' },
  ]);
  gridApi.query();
}

onMounted(() => {
  loadOptions();
});
</script>

<template>
  <Page auto-content-height>
    <PointsAdjustDrawer @success="gridApi.query()" />
    <MemberTagsDrawer @success="gridApi.query()" />
    <Grid :table-title="$t('member.list.title')" />
  </Page>
</template>
