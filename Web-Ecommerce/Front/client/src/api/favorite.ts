import request from './request'
import type { ApiResponse } from '@shared/types'
import type { Product } from '@shared/types/product'

export interface Favorite {
  id: number
  userId: number
  productId: number
  skuId: number
  productName: string
  productImage: string
  price: number
  specDesc: string
  stock: number
  createTime: string
}

export function addFavorite(productId: number, skuId?: number): Promise<ApiResponse<Favorite>> {
  return request.post('/favorites', { productId, skuId })
}

export function removeFavorite(productId: number): Promise<ApiResponse<null>> {
  return request.delete(`/favorites/${productId}`)
}

export function getFavoriteList(): Promise<ApiResponse<Product[]>> {
  return request.get('/favorites')
}

export function checkFavorite(productId: number): Promise<ApiResponse<{ favorited: boolean }>> {
  return request.get(`/favorites/${productId}`)
}

export function batchAddFavorites(items: { productId: number; skuId?: number }[]): Promise<ApiResponse<null>> {
  return request.post('/favorites/batch', { items })
}

export function updateFavoriteSku(productId: number, skuId: number): Promise<ApiResponse<null>> {
  return request.put(`/favorites/${productId}/sku`, { skuId })
}
