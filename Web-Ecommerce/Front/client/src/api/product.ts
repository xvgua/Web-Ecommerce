import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Product, ProductQuery, Category, ProductForm, Review } from '@shared/types/product'

export function getProductList(params: ProductQuery): Promise<ApiResponse<PageResponse<Product>>> {
  return request.get('/products', { params })
}

export function getProductById(id: number): Promise<ApiResponse<Product>> {
  return request.get(`/products/${id}`)
}

export function getProductReviews(
  id: number,
  page = 1,
  pageSize = 10,
  ratingMin?: number,
  ratingMax?: number
): Promise<ApiResponse<PageResponse<Review>>> {
  return request.get(`/products/${id}/reviews`, { params: { page, pageSize, ratingMin, ratingMax } })
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
