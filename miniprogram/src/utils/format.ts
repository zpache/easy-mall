/**
 * 金额格式化：分转元之外，统一保留两位小数
 */
export function formatPrice(value?: number | string | null): string {
  const num = Number(value ?? 0)
  if (Number.isNaN(num)) return '0.00'
  return num.toFixed(2)
}

/**
 * 时间格式化：后端返回 ISO 字符串
 */
export function formatTime(value?: string | null): string {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 19)
}
