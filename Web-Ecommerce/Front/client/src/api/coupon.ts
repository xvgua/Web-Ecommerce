import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Coupon, UserCoupon } from '@shared/types/coupon'

export function getCouponList(page = 1, pageSize = 20): Promise<ApiResponse<PageResponse<Coupon>>> {
  return request.get('/coupons', { params: { page, pageSize } })
}

export function receiveCoupon(id: number): Promise<ApiResponse<null>> {
  return request.post(`/coupons/${id}/receive`)
}

export function getUserCoupons(params: { status?: number; page: number; pageSize: number }): Promise<ApiResponse<PageResponse<UserCoupon>>> {
  return request.get('/user/coupons', { params })
}

export function getAvailableForOrder(amount: number): Promise<ApiResponse<UserCoupon[]>> {
  return request.get('/user/coupons/available', { params: { amount } })
}
