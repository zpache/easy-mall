import { requestClient } from '#/api/request';

export namespace DecorateThemeApi {
  /** 主题配置：固定键位，全部十六进制颜色 */
  export interface ThemeConfig {
    /** 主色：按钮 / 强调 / 选中态 */
    primaryColor: string;
    /** 页面背景色 */
    pageBgColor: string;
    /** 价格强调色 */
    priceColor: string;
    /** 标题文字色 */
    textColor: string;
    /** 次要文字色 */
    subTextColor: string;
  }

  export interface Theme {
    configJson: string;
    id: number;
    isActive: 0 | 1;
    name: string;
    publishedJson?: string;
    remark?: string;
    updatedAt?: string;
    version?: number;
  }

  export interface SavePayload {
    configJson: string;
    name: string;
    remark?: string;
    version?: number;
  }
}

function getDecorateThemes() {
  return requestClient.get<DecorateThemeApi.Theme[]>('/decorate/themes');
}

function createDecorateTheme(data: DecorateThemeApi.SavePayload) {
  return requestClient.post<DecorateThemeApi.Theme>('/decorate/themes', data);
}

function updateDecorateTheme(id: number, data: DecorateThemeApi.SavePayload) {
  return requestClient.put<DecorateThemeApi.Theme>(
    `/decorate/themes/${id}`,
    data,
  );
}

/** 启用即发布：草稿快照生效，切换线上主题 */
function activateDecorateTheme(id: number) {
  return requestClient.put<DecorateThemeApi.Theme>(
    `/decorate/themes/${id}/activate`,
  );
}

function deleteDecorateTheme(id: number) {
  return requestClient.delete(`/decorate/themes/${id}`);
}

export {
  activateDecorateTheme,
  createDecorateTheme,
  deleteDecorateTheme,
  getDecorateThemes,
  updateDecorateTheme,
};
