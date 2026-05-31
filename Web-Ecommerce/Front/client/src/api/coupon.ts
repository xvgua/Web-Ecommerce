import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Coupon } from '@shared/types/coupon'

export function getCouponList(page = 1, pageSize = 20): Promise<ApiResponse<PageResponse<Coupon>>> {
  return request.get('/coupons', { params: { page, pageSize } })
}

export function receiveCoupon(id: number): Promise<ApiResponse<null>> {
  return request.post(`/coupons/${id}/receive`)
}
