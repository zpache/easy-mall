import { $t } from '#/locales';

/**
 * 会员来源渠道（0 公众号 / 1 小程序 / 2 H5 扫码）
 */
export function useSourceOptions() {
  return [
    {
      color: 'processing',
      label: $t('member.source.wechat'),
      value: 0,
    },
    { color: 'success', label: $t('member.source.miniapp'), value: 1 },
    { color: 'warning', label: $t('member.source.h5'), value: 2 },
  ];
}
