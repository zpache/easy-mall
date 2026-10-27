import type { Component } from 'vue';

interface AnalysisOverviewItem {
  /** 数值小数位数，默认 0（销售额等场景可设为 2） */
  decimals?: number;
  icon: Component | string;
  title: string;
  totalTitle: string;
  /** 底部数值：传入 totalText 时显示文字（如环比 +12.5%），否则用数字动画显示 totalValue */
  totalValue?: number;
  totalText?: string;
  totalTextClass?: string;
  value: number;
}

interface WorkbenchProjectItem {
  color?: string;
  content: string;
  date: string;
  group: string;
  icon: Component | string;
  title: string;
  url?: string;
}

interface WorkbenchTrendItem {
  avatar: string;
  content: string;
  date: string;
  title: string;
}

interface WorkbenchTodoItem {
  completed: boolean;
  content: string;
  date: string;
  title: string;
}

interface WorkbenchQuickNavItem {
  color?: string;
  icon: Component | string;
  title: string;
  url?: string;
}

export type {
  AnalysisOverviewItem,
  WorkbenchProjectItem,
  WorkbenchQuickNavItem,
  WorkbenchTodoItem,
  WorkbenchTrendItem,
};
