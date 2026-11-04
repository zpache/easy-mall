<script lang="ts" setup>
import type { AnalysisOverviewItem } from '@vben/common-ui';
import type { EchartsUIType } from '@vben/plugins/echarts';

import { onMounted, ref } from 'vue';

import { AnalysisOverview } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import {
  SvgBellIcon,
  SvgCakeIcon,
  SvgCardIcon,
  SvgDownloadIcon,
} from '@vben/icons';

import {
  ElCard,
  ElRadioButton,
  ElRadioGroup,
} from 'element-plus';

import {
  getDashboardOverviewApi,
  getMemberGrowth,
  getOrderTrend,
  getTopGoods,
} from '#/api/dashboard';

const overviewItems = ref<AnalysisOverviewItem[]>([]);

/** 近 N 天趋势，7/30/90 可切换 */
const trendDays = ref(30);

const trendChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrend } = useEcharts(trendChartRef);

const topGoodsChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTopGoods } = useEcharts(topGoodsChartRef);

const memberChartRef = ref<EchartsUIType>();
const { renderEcharts: renderMember } = useEcharts(memberChartRef);

async function loadTrend() {
  const data = await getOrderTrend(trendDays.value);
  await renderTrend({
    grid: { bottom: 30, left: 60, right: 60, top: 40 },
    legend: { data: ['订单数', '销售额'], top: 0 },
    series: [
      {
        data: data.map((item) => item.orders),
        name: '订单数',
        type: 'bar',
        yAxisIndex: 0,
      },
      {
        data: data.map((item) => Number(item.salesAmount)),
        name: '销售额',
        smooth: true,
        type: 'line',
        yAxisIndex: 1,
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.map((item) => item.date), type: 'category' },
    yAxis: [
      { name: '订单数', type: 'value' },
      { name: '销售额(元)', type: 'value' },
    ],
  });
}

async function loadTopGoods() {
  const data = await getTopGoods(trendDays.value, 10);
  // 倒序让第一名显示在最上方
  const sorted = [...data].reverse();
  await renderTopGoods({
    grid: { bottom: 10, left: 8, right: 30, top: 10, containLabel: true },
    series: [
      {
        data: sorted.map((item) => item.quantity),
        name: '销量',
        type: 'bar',
      },
    ],
    tooltip: {
      formatter: (params: any) => {
        const item = sorted[params?.[0]?.dataIndex];
        return item
          ? `${item.goodsName}<br/>销量 ${item.quantity} · 销售额 ¥${Number(item.salesAmount).toFixed(2)}`
          : '';
      },
      trigger: 'axis',
    },
    xAxis: { type: 'value' },
    yAxis: {
      data: sorted.map((item) =>
        item.goodsName.length > 12
          ? `${item.goodsName.slice(0, 12)}…`
          : item.goodsName,
      ),
      type: 'category',
    },
  });
}

async function loadMemberGrowth() {
  const data = await getMemberGrowth(trendDays.value);
  let total = 0;
  const cumulative = data.map((item) => {
    total += item.newMembers;
    return total;
  });
  await renderMember({
    grid: { bottom: 30, left: 60, right: 30, top: 40 },
    legend: { data: ['新增会员', '累计会员'], top: 0 },
    series: [
      {
        areaStyle: { opacity: 0.15 },
        data: data.map((item) => item.newMembers),
        name: '新增会员',
        smooth: true,
        type: 'line',
      },
      {
        data: cumulative,
        name: '累计会员',
        smooth: true,
        type: 'line',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: { data: data.map((item) => item.date), type: 'category' },
    yAxis: { type: 'value' },
  });
}

/** 环比文案：正数带 +，0 显示「持平」 */
function chainText(rate?: number) {
  if (rate === undefined || rate === null) return '--';
  if (rate > 0) return `+${rate}%`;
  if (rate < 0) return `${rate}%`;
  return '持平';
}

/** 环比颜色：涨红跌绿（国内习惯） */
function chainClass(rate?: number) {
  if (rate === undefined || rate === null || rate === 0) return '';
  return rate > 0 ? 'text-red-500' : 'text-green-500';
}

async function loadOverview() {
  const data = await getDashboardOverviewApi();
  overviewItems.value = [
    {
      icon: SvgCardIcon,
      title: '今日订单',
      totalTitle: '较昨日',
      totalText: chainText(data.ordersChainRate),
      totalTextClass: chainClass(data.ordersChainRate),
      value: data.todayOrders,
    },
    {
      decimals: 2,
      icon: SvgCakeIcon,
      title: '今日销售额',
      totalTitle: '较昨日',
      totalText: chainText(data.salesChainRate),
      totalTextClass: chainClass(data.salesChainRate),
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

async function onDaysChange() {
  await Promise.all([loadTrend(), loadTopGoods(), loadMemberGrowth()]);
}

onMounted(() => {
  loadOverview();
  onDaysChange();
});
</script>

<template>
  <div class="p-4">
    <!-- 紧凑模式：压缩卡片内边距，让「四个指标卡 + 订单趋势」在同一屏内完整呈现 -->
    <AnalysisOverview class="overview-compact" :items="overviewItems" />
    <div class="mb-3 mt-4 flex items-center gap-2">
      <span class="text-sm text-gray-500">统计范围</span>
      <ElRadioGroup v-model="trendDays" size="small" @change="onDaysChange">
        <ElRadioButton :value="7">近 7 天</ElRadioButton>
        <ElRadioButton :value="30">近 30 天</ElRadioButton>
        <ElRadioButton :value="90">近 90 天</ElRadioButton>
      </ElRadioGroup>
    </div>
    <ElCard shadow="never" class="mb-4">
      <template #header>
        <div class="flex items-center justify-between">
          <span>订单趋势</span>
          <span class="text-xs text-gray-400">按下单时间，已排除取消订单</span>
        </div>
      </template>
      <EchartsUI ref="trendChartRef" height="300px" />
    </ElCard>
    <ElCard shadow="never" class="mb-4">
      <template #header>
        <div class="flex items-center justify-between">
          <span>商品销量 TOP10</span>
          <span class="text-xs text-gray-400">按销量降序</span>
        </div>
      </template>
      <EchartsUI ref="topGoodsChartRef" height="360px" />
    </ElCard>
    <ElCard shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span>会员增长</span>
          <span class="text-xs text-gray-400">每日新注册会员与累计曲线</span>
        </div>
      </template>
      <EchartsUI ref="memberChartRef" height="320px" />
    </ElCard>
  </div>
</template>

<style scoped lang="scss">
/* 指标卡紧凑化：CardHeader 原 p-5、CardContent/CardFooter 原 p-6，纵向留白偏大。
   这里只在本页压缩，公共组件 AnalysisOverview 保持原样，其他页面不受影响。 */
.overview-compact {
  gap: 12px;

  // 卡片头部：上下留白收窄，标题与数值之间不再多一层空隙
  :deep(.p-5) {
    gap: 0;
    padding: 12px 16px 0;
  }

  // 数值行与底部说明行：左右 16px，纵向只留必要间距
  :deep(.p-6) {
    padding: 6px 16px 0;
  }

  // 卡片最后一个面板（底部说明行）补回下内边距，避免文字贴边
  :deep(.p-6:last-child) {
    padding-bottom: 12px;
  }
}
</style>
