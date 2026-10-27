<script lang="ts" setup>
import type { AnalysisOverviewItem } from '@vben/common-ui';
import type { TabOption } from '@vben/types';

import { onMounted, ref } from 'vue';

import {
  AnalysisChartCard,
  AnalysisChartsTabs,
  AnalysisOverview,
} from '@vben/common-ui';
import {
  SvgBellIcon,
  SvgCakeIcon,
  SvgCardIcon,
  SvgDownloadIcon,
} from '@vben/icons';

import { getDashboardOverviewApi } from '#/api/dashboard';

import AnalyticsTrends from './analytics-trends.vue';
import AnalyticsVisitsData from './analytics-visits-data.vue';
import AnalyticsVisitsSales from './analytics-visits-sales.vue';
import AnalyticsVisitsSource from './analytics-visits-source.vue';
import AnalyticsVisits from './analytics-visits.vue';

const overviewItems = ref<AnalysisOverviewItem[]>([]);

const chartTabs: TabOption[] = [
  {
    label: '流量趋势',
    value: 'trends',
  },
  {
    label: '月访问量',
    value: 'visits',
  },
];

async function loadOverview() {
  const data = await getDashboardOverviewApi();
  overviewItems.value = [
    {
      icon: SvgCardIcon,
      title: '今日订单',
      totalTitle: '累计订单',
      totalValue: data.totalOrders,
      value: data.todayOrders,
    },
    {
      decimals: 2,
      icon: SvgCakeIcon,
      title: '今日销售额',
      totalTitle: '累计销售额',
      totalValue: data.totalSales,
      value: data.todaySales,
    },
    {
      icon: SvgDownloadIcon,
      title: '待发货订单',
      totalTitle: '已完成订单',
      totalValue: data.finishedOrders,
      value: data.pendingDelivery,
    },
    {
      icon: SvgBellIcon,
      title: '库存预警',
      totalTitle: '在售商品',
      totalValue: data.onSaleGoods,
      value: data.stockWarning,
    },
  ];
}

onMounted(() => {
  loadOverview();
});
</script>

<template>
  <div class="p-5">
    <AnalysisOverview :items="overviewItems" />
    <AnalysisChartsTabs :tabs="chartTabs" class="mt-5">
      <template #trends>
        <AnalyticsTrends />
      </template>
      <template #visits>
        <AnalyticsVisits />
      </template>
    </AnalysisChartsTabs>

    <div class="mt-5 w-full md:flex">
      <AnalysisChartCard class="mt-5 md:mt-0 md:mr-4 md:w-1/3" title="访问数量">
        <AnalyticsVisitsData />
      </AnalysisChartCard>
      <AnalysisChartCard class="mt-5 md:mt-0 md:mr-4 md:w-1/3" title="访问来源">
        <AnalyticsVisitsSource />
      </AnalysisChartCard>
      <AnalysisChartCard class="mt-5 md:mt-0 md:w-1/3" title="销售概览">
        <AnalyticsVisitsSales />
      </AnalysisChartCard>
    </div>
  </div>
</template>
