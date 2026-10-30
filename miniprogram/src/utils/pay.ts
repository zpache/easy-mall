import { orderApi } from '@/api/order'

/**
 * 统一支付入口。
 *
 * - 后端未启用微信支付但开启模拟支付时，`prepay` 已直接完成入账，返回 'mock'；
 * - 启用微信支付时，用返回的参数调起 wx.requestPayment，返回 'wxpay'；
 * - 非微信环境（H5 预览）无法调起，抛错由调用方提示。
 *
 * 支付结果以服务端回调为准，调用方拿到 resolve 后应重新拉取订单详情确认状态。
 */
export async function payOrder(orderId: number): Promise<'mock' | 'wxpay'> {
  const params = await orderApi.prepay(orderId)
  if (params.mock) {
    return 'mock'
  }
  // #ifdef MP-WEIXIN
  await uni.requestPayment({
    provider: 'wxpay',
    timeStamp: params.timeStamp ?? '',
    nonceStr: params.nonceStr ?? '',
    package: params.package ?? '',
    signType: 'RSA' as any,
    paySign: params.paySign ?? ''
  })
  return 'wxpay'
  // #endif
  // #ifndef MP-WEIXIN
  throw new Error('NOT_IN_WECHAT')
  // #endif
}
