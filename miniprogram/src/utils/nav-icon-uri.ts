import { navIconBody } from './nav-icons'

const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

/** 小程序运行时没有 btoa，这里对纯 ASCII 内容做标准 base64 编码 */
function base64(input: string): string {
  let output = ''
  for (let i = 0; i < input.length; i += 3) {
    const c1 = input.charCodeAt(i)
    const c2 = input.charCodeAt(i + 1)
    const c3 = input.charCodeAt(i + 2)
    output += BASE64_CHARS[c1 >> 2]
    output += BASE64_CHARS[((c1 & 3) << 4) | (isNaN(c2) ? 0 : c2 >> 4)]
    output += isNaN(c2) ? '=' : BASE64_CHARS[((c2 & 15) << 2) | (isNaN(c3) ? 0 : c3 >> 6)]
    output += isNaN(c3) ? '=' : BASE64_CHARS[c3 & 63]
  }
  return output
}

/** 图标字段既可能是图片地址（历史数据），也可能是 lucide:xxx 图标名 */
export function isImageUrl(value?: string): boolean {
  return /^(?:https?:)?\/\//.test(value || '')
}

const uriCache: Record<string, string> = {}

/**
 * 图标名 -> SVG data URI（微信小程序 image 支持 base64 svg）。
 * 颜色参与缓存 key，选中/未选中各缓存一份。
 */
export function navIconUri(name?: string, color = '#909399'): string {
  const body = navIconBody(name)
  if (!body) return ''
  const key = `${name}|${color}`
  if (uriCache[key]) return uriCache[key]
  const svg =
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" ' +
    `stroke="${color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">` +
    `${body.replace(/CURRENT_COLOR/g, color)}</svg>`
  const uri = `data:image/svg+xml;base64,${base64(svg)}`
  uriCache[key] = uri
  return uri
}

/** 统一解析图标来源：图片地址原样返回，图标名转 SVG，无法识别返回空串 */
export function iconSource(value: string | undefined, color: string, fallbackName = ''): string {
  if (isImageUrl(value)) return value || ''
  return navIconUri(value || fallbackName, color)
}
