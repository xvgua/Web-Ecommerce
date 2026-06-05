import request from './request'
import type { ApiResponse } from '@shared/types'
import type { SeckillActivity, SeckillProduct, SeckillOrderRequest } from '@shared/types/seckill'
import type { Order } from '@shared/types/order'

export function getActiveActivities(): Promise<ApiResponse<SeckillActivity[]>> {
  return request.get('/seckill/activities')
}

export function getAllActivities(): Promise<ApiResponse<SeckillActivity[]>> {
  return request.get('/seckill/activities/all')
}

export function getActivityDetail(id: number): Promise<ApiResponse<SeckillActivity>> {
  return request.get(`/seckill/activities/${id}`)
}

export function getSeckillProductById(id: number): Promise<ApiResponse<SeckillProduct>> {
  return request.get(`/seckill/product/${id}`)
}

export function getMyPurchasedSeckill(): Promise<ApiResponse<number[]>> {
  // TODO: 迁移至 order-service 查询用户秒杀购买记录
  return Promise.resolve({ code: 200, message: 'success', data: [] } as ApiResponse<number[]>)
}

export function createSeckillOrder(data: SeckillOrderRequest): Promise<ApiResponse<Order>> {
  // 秒杀下单改为调 order-service 创建订单端点
  return request.post('/orders', {
    productId: data.seckillProductId,
    addressId: data.addressId,
    remark: data.remark,
    quantity: 1,
  })
}
