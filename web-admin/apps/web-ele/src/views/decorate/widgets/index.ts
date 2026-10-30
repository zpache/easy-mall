import type { PageSchema, WidgetCategory, WidgetDefinition } from './types';

import BannerWidget from './components/BannerWidget.vue';
import BlankWidget from './components/BlankWidget.vue';
import CouponWidget from './components/CouponWidget.vue';
import GoodsWidget from './components/GoodsWidget.vue';
import ImageWidget from './components/ImageWidget.vue';
import NavWidget from './components/NavWidget.vue';
import NoticeWidget from './components/NoticeWidget.vue';
import RichTextWidget from './components/RichTextWidget.vue';
import SearchWidget from './components/SearchWidget.vue';
import SeckillWidget from './components/SeckillWidget.vue';
import ShopInfoWidget from './components/ShopInfoWidget.vue';
import TitleWidget from './components/TitleWidget.vue';

export const CATEGORY_LABELS: Record<WidgetCategory, string> = {
  base: '基础组件',
  content: '内容组件',
  goods: '商品组件',
  marketing: '营销组件',
};

const alignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
];

export const WIDGETS: WidgetDefinition[] = [
  {
    category: 'base',
    component: SearchWidget,
    defaultProps: {
      align: 'left',
      bgColor: '#ffffff',
      margin: 10,
      placeholder: '搜索商品 / 品牌',
      radius: 18,
    },
    fields: [
      { key: 'placeholder', label: '提示文字', type: 'input', placeholder: '搜索商品' },
      { key: 'align', label: '对齐方式', type: 'select', options: alignOptions },
      { key: 'radius', label: '圆角', type: 'number', min: 0, max: 24 },
      { key: 'margin', label: '外边距', type: 'number', min: 0, max: 30 },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:search',
    name: '搜索框',
    type: 'search',
  },
  {
    category: 'base',
    component: NoticeWidget,
    defaultProps: {
      bgColor: '#fff7e8',
      color: '#ff6b00',
      text: '新客首单立减 10 元，满 99 包邮',
    },
    fields: [
      { key: 'text', label: '公告内容', type: 'input' },
      { key: 'color', label: '文字颜色', type: 'color' },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:megaphone',
    name: '公告栏',
    type: 'notice',
  },
  {
    category: 'base',
    component: BlankWidget,
    defaultProps: { bgColor: '#f5f6f7', height: 12 },
    fields: [
      { key: 'height', label: '高度', type: 'number', min: 4, max: 200 },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:move-vertical',
    name: '辅助空白',
    type: 'blank',
  },
  {
    category: 'content',
    component: BannerWidget,
    defaultProps: {
      height: 150,
      images: [
        { link: '', url: 'https://picsum.photos/seed/banner-a/750/300' },
        { link: '', url: 'https://picsum.photos/seed/banner-b/750/300' },
      ],
      interval: 3,
      margin: 12,
      radius: 8,
    },
    fields: [
      { key: 'height', label: '图片高度', type: 'number', min: 60, max: 400, tip: '单位 px，宽度自适应手机' },
      { key: 'radius', label: '圆角', type: 'number', min: 0, max: 24 },
      { key: 'margin', label: '外边距', type: 'number', min: 0, max: 30 },
      {
        itemFields: [
          { key: 'url', label: '图片', type: 'image' },
          { key: 'link', label: '跳转链接', type: 'input', placeholder: '/goods/1' },
        ],
        itemLabelKey: 'url',
        key: 'images',
        label: '轮播图片',
        maxItems: 8,
        type: 'items',
      },
    ],
    icon: 'lucide:gallery-horizontal',
    name: '轮播图',
    type: 'banner',
  },
  {
    category: 'content',
    component: ImageWidget,
    defaultProps: {
      height: 120,
      margin: 12,
      radius: 8,
      url: 'https://picsum.photos/seed/ad-single/750/300',
    },
    fields: [
      { key: 'url', label: '图片', type: 'image' },
      { key: 'height', label: '高度', type: 'number', min: 40, max: 400 },
      { key: 'radius', label: '圆角', type: 'number', min: 0, max: 24 },
      { key: 'margin', label: '外边距', type: 'number', min: 0, max: 30 },
      { key: 'link', label: '跳转链接', type: 'input', placeholder: '/goods/1' },
    ],
    icon: 'lucide:image',
    name: '单图广告',
    type: 'image',
  },
  {
    category: 'base',
    component: NavWidget,
    defaultProps: {
      bgColor: '#ffffff',
      columns: 5,
      items: [
        { icon: 'https://picsum.photos/seed/nav-a/80/80', link: '/category/1', text: '新鲜水果' },
        { icon: 'https://picsum.photos/seed/nav-b/80/80', link: '/category/2', text: '肉禽蛋奶' },
        { icon: 'https://picsum.photos/seed/nav-c/80/80', link: '/category/3', text: '休闲零食' },
        { icon: 'https://picsum.photos/seed/nav-d/80/80', link: '/category/4', text: '粮油调味' },
        { icon: 'https://picsum.photos/seed/nav-e/80/80', link: '/category', text: '全部分类' },
      ],
    },
    fields: [
      {
        key: 'columns',
        label: '每行个数',
        options: [
          { label: '4 个', value: 4 },
          { label: '5 个', value: 5 },
        ],
        type: 'select',
      },
      {
        itemFields: [
          { key: 'icon', label: '图标', type: 'image' },
          { key: 'text', label: '文字', type: 'input' },
          { key: 'link', label: '跳转链接', type: 'input', placeholder: '/category/1' },
        ],
        itemLabelKey: 'text',
        key: 'items',
        label: '导航项',
        maxItems: 10,
        type: 'items',
      },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:layout-grid',
    name: '图标导航',
    type: 'nav',
  },
  {
    category: 'content',
    component: TitleWidget,
    defaultProps: {
      align: 'left',
      bold: true,
      color: '#333333',
      margin: 12,
      more: true,
      size: 16,
      subtitle: '',
      text: '为你推荐',
    },
    fields: [
      { key: 'text', label: '标题', type: 'input' },
      { key: 'subtitle', label: '副标题', type: 'input' },
      { key: 'align', label: '对齐方式', type: 'select', options: alignOptions },
      { key: 'size', label: '字号', type: 'number', min: 12, max: 32 },
      { key: 'color', label: '文字颜色', type: 'color' },
      { key: 'bold', label: '加粗', type: 'switch' },
      { key: 'more', label: '显示"更多"', type: 'switch' },
      {
        key: 'moreLink',
        label: '更多跳转链接',
        tip: '留空默认进入全部商品列表；也可填 /goods/1、/category/2、/pages/...',
        type: 'input',
      },
      { key: 'margin', label: '外边距', type: 'number', min: 0, max: 40 },
    ],
    icon: 'lucide:heading',
    name: '标题',
    type: 'title',
  },
  {
    category: 'content',
    component: RichTextWidget,
    defaultProps: {
      color: '#333333',
      content: '<p>这里是一段富文本内容，可以放活动说明、配送规则等。</p>',
      margin: 12,
      size: 13,
    },
    fields: [
      {
        key: 'content',
        label: '内容（支持 HTML）',
        tip: '支持 p / strong / a / img 等标签',
        type: 'textarea',
      },
      { key: 'size', label: '字号', type: 'number', min: 10, max: 24 },
      { key: 'color', label: '文字颜色', type: 'color' },
      { key: 'margin', label: '外边距', type: 'number', min: 0, max: 40 },
    ],
    icon: 'lucide:type',
    name: '富文本',
    type: 'richText',
  },
  {
    category: 'goods',
    component: GoodsWidget,
    defaultProps: {
      columns: 2,
      goods: [],
      layout: 'grid',
      showSales: true,
    },
    fields: [
      {
        key: 'layout',
        label: '展示方式',
        options: [
          { label: '双列网格', value: 'grid' },
          { label: '单列列表', value: 'list' },
        ],
        type: 'select',
      },
      {
        key: 'columns',
        label: '每行个数',
        options: [
          { label: '2 个', value: 2 },
          { label: '3 个', value: 3 },
        ],
        type: 'select',
      },
      { key: 'showSales', label: '显示销量', type: 'switch' },
      {
        key: 'goods',
        label: '选择商品',
        maxItems: 12,
        tip: '展示数据为选择时的商品快照',
        type: 'goods',
      },
    ],
    icon: 'lucide:package',
    name: '商品列表',
    type: 'goods',
  },
  {
    category: 'marketing',
    component: SeckillWidget,
    defaultProps: {
      bgColor: '#fff1f0',
      endTime: '2026-12-31 22:00:00',
      goods: [],
      subtitle: '每天 10 点开抢',
      title: '限时秒杀',
    },
    fields: [
      { key: 'title', label: '标题', type: 'input' },
      { key: 'subtitle', label: '副标题', type: 'input' },
      {
        key: 'goods',
        label: '秒杀商品',
        maxItems: 10,
        tip: '展示商品现价，与商品详情页价格一致',
        type: 'goods',
      },
      { key: 'endTime', label: '结束时间', type: 'datetime' },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:zap',
    name: '限时秒杀',
    type: 'seckill',
  },
  {
    category: 'marketing',
    component: CouponWidget,
    defaultProps: {
      bgColor: '#ffffff',
      couponCodes: [],
      title: '领券中心',
    },
    fields: [
      { key: 'title', label: '标题', type: 'input' },
      {
        key: 'couponCodes',
        label: '优惠券',
        max: 6,
        tip: '来自营销中心，最多 6 张',
        type: 'coupons',
      },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:ticket-percent',
    name: '优惠券',
    type: 'coupon',
  },
  {
    category: 'goods',
    component: ShopInfoWidget,
    defaultProps: {
      bgColor: '#ffffff',
      desc: '生鲜果蔬 · 次日达',
      logo: 'https://picsum.photos/seed/shop-logo-a/120/120',
      name: '易商城旗舰店',
      phone: '400-800-1234',
    },
    fields: [
      { key: 'logo', label: '店铺 Logo', type: 'image' },
      { key: 'name', label: '店铺名称', type: 'input' },
      { key: 'desc', label: '店铺简介', type: 'input' },
      { key: 'phone', label: '客服电话', type: 'input' },
      { key: 'bgColor', label: '背景色', type: 'color' },
    ],
    icon: 'lucide:store',
    name: '店铺信息',
    type: 'shopInfo',
  },
];

export const WIDGET_MAP = new Map(WIDGETS.map((item) => [item.type, item]));

export function getWidget(type: string): WidgetDefinition | undefined {
  return WIDGET_MAP.get(type as any);
}

export function createComponent(type: string) {
  const widget = getWidget(type);
  if (!widget) {
    return null;
  }
  return {
    id: `c_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`,
    props: JSON.parse(JSON.stringify(widget.defaultProps)),
    type: widget.type,
    visible: true,
  };
}

/** 新建页面时的初始结构：空白画布 */
export function createEmptySchema(title = '商城首页'): PageSchema {
  return {
    components: [],
    page: { bgColor: '#f5f6f7', bgImage: '', padding: 0, title },
  };
}

/** 兼容历史/异常数据：补齐缺省字段 */
export function normalizeSchema(raw: any, title = ''): PageSchema {
  const base = createEmptySchema(title);
  if (!raw || typeof raw !== 'object') {
    return base;
  }
  const components = Array.isArray(raw.components)
    ? raw.components
        .filter((item: any) => item && getWidget(item.type))
        .map((item: any) => ({
          id: item.id || `c_${Math.random().toString(36).slice(2, 10)}`,
          props: { ...(getWidget(item.type)?.defaultProps || {}), ...(item.props || {}) },
          type: item.type,
          visible: item.visible !== false,
        }))
    : [];
  return {
    components,
    page: { ...base.page, ...(raw.page || {}), title: raw.page?.title || title || base.page.title },
  };
}
