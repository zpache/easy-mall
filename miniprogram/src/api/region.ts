import { buildQuery, request } from './request'
import type { RegionOption } from '@/types/api'

export const regionApi = {
  /** 查询子区域：parentCode=0 为省级，逐级懒加载 */
  children: (parentCode: string) =>
    request<RegionOption[]>({ url: `/regions${buildQuery({ parentCode })}` })
}
