import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Product, ProductQuery, Category, ProductForm } from '@shared/types/product'

export function getProductList(params: ProductQuery): Promise<ApiResponse<PageResponse<Product>>> {
  return request.get('/products', { params })
}

export function getProductById(id: number): Promise<ApiResponse<Product>> {
  return request.get(`/products/${id}`)
}

export function getCategories(): Promise<ApiResponse<Category[]>> {
  return request.get('/categories')
}

export function getHotProducts(limit = 8): Promise<ApiResponse<Product[]>> {
  return request.get('/products/hot', { params: { limit } })
}

export function getNewProducts(limit = 8): Promise<ApiResponse<Product[]>> {
  return request.get('/products/new', { params: { limit } })
}
