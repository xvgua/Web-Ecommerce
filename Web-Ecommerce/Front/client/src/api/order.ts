import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Order, OrderQuery, CreateOrderForm, PayIntent, PayStatus } from '@shared/types/order'

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

export function createPayIntent(id: number, payMethod: string): Promise<ApiResponse<PayIntent>> {
  return request.post(`/orders/${id}/pay-intent`, { payMethod })
}

export function getPayStatus(id: number): Promise<ApiResponse<PayStatus>> {
  return request.get(`/orders/${id}/pay-status`)
}

export function simulateScan(id: number): Promise<ApiResponse<PayStatus>> {
  return request.post(`/orders/${id}/scan-simulate`)
}

export function confirmPay(id: number): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/pay/confirm`)
}

export function confirmReceive(id: number): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/confirm`)
}

export function remindShip(id: number): Promise<ApiResponse<null>> {
  return request.post(`/orders/${id}/remind`)
}

export function refundOrder(id: number): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/refund`)
}

export function reorderOrder(id: number): Promise<ApiResponse<null>> {
  return request.post(`/orders/${id}/reorder`)
}

export function getLogistics(id: number): Promise<ApiResponse<any>> {
  return request.get(`/orders/${id}/logistics`)
}

export function updateOrderAddress(id: number, data: { addressId: number }): Promise<ApiResponse<null>> {
  return request.put(`/orders/${id}/address`, data)
}
