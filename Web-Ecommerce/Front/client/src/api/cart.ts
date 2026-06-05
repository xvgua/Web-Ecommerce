import request from './request'
import type { ApiResponse } from '@shared/types'
import type { CartItem, AddToCartForm, UpdateCartForm } from '@shared/types/cart'

export function getCartList(skipErrorToast = false): Promise<ApiResponse<CartItem[]>> {
  return request.get('/cart', { _skipErrorToast: skipErrorToast } as any)
}

export function addToCart(data: AddToCartForm): Promise<ApiResponse<null>> {
  return request.post('/cart', data)
}

export function updateCartItem(data: UpdateCartForm): Promise<ApiResponse<null>> {
  return request.put(`/cart/${data.id}`, data)
}

export function removeCartItem(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/cart/${id}`)
}

export function clearCart(): Promise<ApiResponse<null>> {
  return request.delete('/cart/clear')
}
