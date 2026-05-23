import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Order, OrderQuery, CreateOrderForm } from '@shared/types/order'

export function getOrderList(params: OrderQuery): Promise<ApiResponse<PageResponse<Order>>> {
  return request.get('/orders', { params })
}

export function getOrderById(id: number): Promise<ApiResponse<Order>> {
  return request.get(`/orders/${id}`)
}

export function createOrder(data: CreateOrderForm): Promise<ApiResponse<Order>> {
  return request.post('/orders', data)
}

export function cancelOrder(id: number): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/cancel`)
}

export function payOrder(id: number, payMethod: string): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/pay`, { payMethod })
}

export function confirmReceive(id: number): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/confirm`)
}
