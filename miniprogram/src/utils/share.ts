import { getUid } from '@/utils/storage'
import { resolveMediaUrl } from '@/utils/url'

/**
 * 分享转发统一能力：
 * - 每张分享卡片都带上分享人 ID（inviter），进入方落地后由 captureInviter 记录，用于拉新归因
 * - 转发给好友（onShareAppMessage）与分享到朋友圈（onShareTimeline）走同一套路径/标题口径
 * - 图片地址统一补全为绝对地址，相对路径的素材（/files/xxx）在微信里也能正常展示
 */

/** 邀请人缓存：首次带 inviter 进入时写入，后续不再覆盖 */
const INVITER_KEY = 'easy_mall_inviter'

/** 拿不到装修标题时的店铺兜底名 */
export const SHOP_NAME = '易购商城'

/** 分享 query 参数：undefined / null / 空串会被忽略 */
type QueryParams = Record<string, string | number | undefined | null>

export interface ShareAppMessage {
  title: string
  path: string
  imageUrl?: string
}

export interface ShareTimelineMessage {
  title: string
  query?: string
  imageUrl?: string
}

/**
 * 记录分享邀请人。规则：
 * - 只认纯数字用户 ID，避免任意 query 参数污染缓存
 * - 已有邀请人时不覆盖：老用户被再次分享，归因仍算第一次带来他的那个人
 * - 自己分享给自己（自己点开自己的卡片）不计入
 */
export function captureInviter(query?: Record<string, any> | null): void {
  if (!query) return
  const inviter = String(query.inviter ?? query.inviterId ?? query.shareUid ?? query.inv ?? '').trim()
  if (!/^\d+$/.test(inviter)) return
  const self = getUid()
  if (self && String(self) === inviter) return
  if (uni.getStorageSync(INVITER_KEY)) return
  uni.setStorageSync(INVITER_KEY, Number(inviter))
}

/**
 * 解析小程序码 scene 参数（getwxacodeunlimit 落地时 query.scene 为 URL 编码的 a=1&b=2）。
 * 返回 null 表示没有 scene；解析结果可直接交给 captureInviter / 当业务参数用。
 */
export function parseScene(scene?: string): Record<string, string> | null {
  if (!scene) return null
  const decoded = decodeURIComponent(scene)
  if (!decoded.includes('=')) return null
  const params: Record<string, string> = {}
  for (const pair of decoded.split('&')) {
    const index = pair.indexOf('=')
    if (index <= 0) continue
    params[pair.slice(0, index).trim()] = pair.slice(index + 1).trim()
  }
  return Object.keys(params).length ? params : null
}

/** 当前设备记录的邀请人（0 表示没有） */
export function getInviter(): number {
  return Number(uni.getStorageSync(INVITER_KEY)) || 0
}

/** 清空邀请人缓存（联调归因逻辑时可手动调用） */
export function clearInviter(): void {
  uni.removeStorageSync(INVITER_KEY)
}

function stringify(params?: QueryParams): string {
  if (!params) return ''
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && String(value) !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
}

/**
 * 生成分享路径：自动附上当前用户 ID 作为 inviter。
 * 未登录时只拼业务参数，不生成空的 inviter=。
 */
export function sharePath(path: string, params?: QueryParams): string {
  const query = stringify({ ...params, inviter: getUid() || undefined })
  if (!query) return path
  return `${path}${path.includes('?') ? '&' : '?'}${query}`
}

/** 转发给好友的卡片配置 */
export function appMessage(title: string, path: string, imageUrl?: string): ShareAppMessage {
  const message: ShareAppMessage = { title: title || SHOP_NAME, path }
  const image = resolveMediaUrl(imageUrl)
  if (image) message.imageUrl = image
  return message
}

/** 分享到朋友圈的配置：路径固定为当前页，只能自定义 query */
export function timelineMessage(
  title: string,
  params?: QueryParams,
  imageUrl?: string
): ShareTimelineMessage {
  const message: ShareTimelineMessage = { title: title || SHOP_NAME }
  const query = stringify({ ...params, inviter: getUid() || undefined })
  if (query) message.query = query
  const image = resolveMediaUrl(imageUrl)
  if (image) message.imageUrl = image
  return message
}

/**
 * 打开右上角菜单里的「转发」「分享到朋友圈」入口。
 * 小程序默认只开转发，朋友圈需要显式 showShareMenu 才出现。
 */
export function enableShareMenu(): void {
  // #ifdef MP-WEIXIN
  uni.showShareMenu({
    withShareTicket: true,
    menus: ['shareAppMessage', 'shareTimeline']
  })
  // #endif
}
