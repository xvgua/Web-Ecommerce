import request from './request'
import type { ApiResponse } from '@shared/types'
import type { Banner } from '@shared/types'

export function getBanners(): Promise<ApiResponse<Banner[]>> {
  return request.get('/banners')
}
