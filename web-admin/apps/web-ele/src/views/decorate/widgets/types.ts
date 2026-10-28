/**
 * 装修组件模型：画布上的组件、属性字段描述与组件定义
 */

export type WidgetType =
  | 'banner'
  | 'blank'
  | 'coupon'
  | 'goods'
  | 'image'
  | 'nav'
  | 'notice'
  | 'richText'
  | 'search'
  | 'seckill'
  | 'shopInfo'
  | 'title';

export type WidgetCategory = 'base' | 'content' | 'goods' | 'marketing';

export interface PageConfig {
  /** 页面标题（手机状态栏下方不显示，仅用于预览页标题） */
  bgColor: string;
  bgImage: string;
  padding: number;
  title: string;
}

export interface CanvasComponent {
  id: string;
  props: Record<string, any>;
  type: WidgetType;
  visible: boolean;
}

export interface PageSchema {
  components: CanvasComponent[];
  page: PageConfig;
}

/** 属性面板字段类型 */
export type PropFieldType =
  | 'color'
  | 'datetime'
  | 'image'
  | 'input'
  | 'items'
  | 'number'
  | 'select'
  | 'switch'
  | 'textarea';

export interface PropField {
  /** 绑定的 props 键 */
  key: string;
  label: string;
  type: PropFieldType;
  max?: number;
  min?: number;
  options?: { label: string; value: any }[];
  placeholder?: string;
  step?: number;
  /** items 类型：每行可编辑的子字段 */
  itemFields?: PropField[];
  itemLabelKey?: string;
  maxItems?: number;
  tip?: string;
}

export interface WidgetDefinition {
  category: WidgetCategory;
  component: any;
  defaultProps: Record<string, any>;
  fields: PropField[];
  icon: string;
  name: string;
  type: WidgetType;
}
