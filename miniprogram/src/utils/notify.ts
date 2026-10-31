import { notifyApi } from '@/api/notify'

/**
 * 订单订阅消息授权：支付成功后（用户点击链路内）请求订阅
 * 「发货提醒 / 退款结果」模板。一次性订阅每授权一次可推送一条，
 * 用户勾选「总是保持以上选择」后无需再次弹窗。
 *
 * 非微信环境（H5）或不支持时静默跳过；失败不打断支付主流程。
 */
export async function requestOrderSubscribe() {
  // #ifdef MP-WEIXIN
  try {
    const tmplIds = await notifyApi.subscribeTemplates()
    if (!tmplIds.length) return
    await uni.requestSubscribeMessage({ tmplIds })
  } catch {
    // 用户拒绝 / 请求失败均静默，不影响支付结果
  }
  // #endif
}
