import { request } from './request'

/**
 * 分享相关接口：商品小程序码（分享海报用）。
 * 后端按「商品 + 邀请人」落盘缓存，重复生成不会反复调用微信接口。
 */
export const shareApi = {
  /** 商品小程序码图片地址（/files/... 相对路径，用 resolveMediaUrl 补全） */
  goodsQrcode: (goodsId: number) =>
    request<string>({ url: `/share/qrcode/goods/${goodsId}` })
}
