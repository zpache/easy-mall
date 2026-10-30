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

/**
 * 倒计时格式化：秒 → mm:ss（超过 1 小时补上小时位 h:mm:ss）
 * 负数/0 统一返回 00:00，由调用方决定超时后的表现
 */
export function formatCountdown(totalSeconds?: number | null): string {
  const seconds = Math.max(Math.floor(Number(totalSeconds ?? 0)), 0)
  const pad = (value: number) => String(value).padStart(2, '0')
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`
}
