import { defineOverridesPreferences } from '@vben/preferences';

/**
 * @description 项目配置文件
 * 只需要覆盖项目中的一部分配置，不需要的配置不用覆盖，会自动使用默认配置
 * !!! 更改配置后请清空缓存，否则可能不生效
 */
export const overridesPreferences = defineOverridesPreferences({
  // overrides
  app: {
    name: import.meta.env.VITE_APP_TITLE,
    // 菜单与路由完全由后端 /system/menu/routes 下发（登录后动态加载）
    accessMode: 'backend',
  },
  theme: {
    // 默认浅色主题（浏览器缓存过旧偏好的需在偏好设置里点「恢复默认」或清缓存）
    mode: 'light',
  },
});
