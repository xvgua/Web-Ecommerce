import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Product, ProductQuery, Category, ProductForm, Review, ReviewComment } from '@shared/types/product'

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

export function createReview(data: {
  productId: number
  orderId: number
  ratingDesc: number
  ratingLogistics: number
  ratingService: number
  content: string
  images: string[]
}): Promise<ApiResponse<Review>> {
  return request.post('/reviews', data)
}

export function createFollowUpReview(data: {
  productId: number
  orderId: number
  content: string
  images: string[]
}): Promise<ApiResponse<Review>> {
  return request.post(`/reviews/${data.productId}/followup`, data)
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

export function getMyReviews(page = 1, pageSize = 20): Promise<ApiResponse<PageResponse<Review>>> {
  return request.get('/reviews/mine', { params: { page, pageSize } })
}

export function likeReview(id: number): Promise<ApiResponse<{ liked: boolean }>> {
  return request.post(`/reviews/${id}/like`)
}

export function unlikeReview(id: number): Promise<ApiResponse<{ unliked: boolean }>> {
  return request.delete(`/reviews/${id}/like`)
}

export function getReviewComments(reviewId: number): Promise<ApiResponse<ReviewComment[]>> {
  return request.get(`/reviews/${reviewId}/comments`)
}

export function addReviewComment(reviewId: number, content: string): Promise<ApiResponse<ReviewComment>> {
  return request.post(`/reviews/${reviewId}/comments`, { content })
}
