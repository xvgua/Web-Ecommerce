import request from './request'
import type { ApiResponse } from '@shared/types'
import type { SeckillActivity, SeckillProduct, SeckillOrderRequest } from '@shared/types/seckill'
import type { Order } from '@shared/types/order'

export function getActiveActivities(): Promise<ApiResponse<SeckillActivity[]>> {
  return request.get('/seckill/activities')
}

export function getActivityDetail(id: number): Promise<ApiResponse<SeckillActivity>> {
  return request.get(`/seckill/activities/${id}`)
}

export function getSeckillProductById(id: number): Promise<ApiResponse<SeckillProduct>> {
  return request.get(`/seckill/product/${id}`)
}

export function getMyPurchasedSeckill(): Promise<ApiResponse<number[]>> {
  return request.get('/seckill/my-purchased')
}

export function createSeckillOrder(data: SeckillOrderRequest): Promise<ApiResponse<Order>> {
  return request.post('/seckill/order', data)
}
