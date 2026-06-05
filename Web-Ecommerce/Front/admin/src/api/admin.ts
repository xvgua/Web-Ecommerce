import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Product, ProductQuery, Category, ProductForm, HotKeyword } from '@shared/types/product'
import type { Order, OrderQuery } from '@shared/types/order'
import type { User } from '@shared/types/user'
import type { Coupon, CouponForm } from '@shared/types/coupon'
import type { SeckillActivity, SeckillActivityForm } from '@shared/types/seckill'
import type { Feedback } from '@shared/types/feedback'
import type { Review, ReviewComment } from '@shared/types/product'
import type { Announcement, Banner } from '@shared/types'

// ===== Auth =====
export function adminLogin(data: { username: string; password: string }): Promise<ApiResponse<{ token: string }>> {
  return request.post('/admin/auth/login', data)
}

// ===== Dashboard =====
export function getDashboardStats(): Promise<ApiResponse<{
  totalUsers: number
  totalOrders: number
  totalSales: number
  todayOrders: number
  todaySales: number
  pendingOrders: number
  shippingOrders: number
  completedOrders: number
  cancelledOrders: number
}>> {
  return request.get('/admin/dashboard/stats')
}

export interface SalesTrendItem {
  date: string
  orderCount: number
  salesAmount: number
}

export function getSalesTrend(range: string): Promise<ApiResponse<SalesTrendItem[]>> {
  return request.get('/admin/dashboard/sales-trend', { params: { range } })
}

export interface HotProductItem {
  id: number
  name: string
  mainImage: string
  sales: number
  salesAmount: number
}

export function getHotProducts(range: string, top?: number): Promise<ApiResponse<HotProductItem[]>> {
  return request.get('/admin/dashboard/hot-products', { params: { range, top } })
}

// ===== Product Management =====
export function getProductList(params: ProductQuery): Promise<ApiResponse<PageResponse<Product>>> {
  return request.get('/admin/products', { params })
}

export function getProductById(id: number): Promise<ApiResponse<Product>> {
  return request.get(`/admin/products/${id}`)
}

export function createProduct(data: ProductForm): Promise<ApiResponse<Product>> {
  return request.post('/admin/products', data)
}

export function updateProduct(id: number, data: ProductForm): Promise<ApiResponse<null>> {
  return request.put(`/admin/products/${id}`, data)
}

export function deleteProduct(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/products/${id}`)
}

export function toggleSkuStatus(productId: number, skuId: number, status: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/products/${productId}/skus/${skuId}/status`, { status })
}

export function deleteSku(productId: number, skuId: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/products/${productId}/skus/${skuId}`)
}

export function getCategoryList(): Promise<ApiResponse<Category[]>> {
  return request.get('/admin/categories')
}

export function createCategory(data: Partial<Category>): Promise<ApiResponse<Category>> {
  return request.post('/admin/categories', data)
}

export function updateCategory(id: number, data: Partial<Category>): Promise<ApiResponse<null>> {
  return request.put(`/admin/categories/${id}`, data)
}

export function deleteCategory(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/categories/${id}`)
}

export function moveCategorySort(id: number, direction: 'up' | 'down'): Promise<ApiResponse<null>> {
  return request({ method: 'PUT', url: `/admin/categories/${id}/move?direction=${direction}` })
}

// ===== Order Management =====
export function getOrderList(params: OrderQuery): Promise<ApiResponse<PageResponse<Order>>> {
  return request.get('/admin/orders', { params })
}

export function getOrderById(id: number): Promise<ApiResponse<Order>> {
  return request.get(`/admin/orders/${id}`)
}

export function shipOrder(id: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/orders/${id}/ship`)
}

export function cancelOrder(id: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/orders/${id}/cancel`)
}

// ===== User Management =====
export function getUserList(params: PageQuery): Promise<ApiResponse<PageResponse<User>>> {
  return request.get('/admin/users', { params })
}

export function toggleUserStatus(id: number, status: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/users/${id}/status`, { status })
}

// ===== Coupon Management =====
export function getCouponList(params: PageQuery & { keyword?: string; type?: number; status?: number }): Promise<ApiResponse<PageResponse<Coupon>>> {
  return request.get('/admin/coupons', { params })
}

export function getCouponById(id: number): Promise<ApiResponse<Coupon>> {
  return request.get(`/admin/coupons/${id}`)
}

export function createCoupon(data: CouponForm): Promise<ApiResponse<Coupon>> {
  return request.post('/admin/coupons', data)
}

export function updateCoupon(id: number, data: CouponForm): Promise<ApiResponse<null>> {
  return request.put(`/admin/coupons/${id}`, data)
}

export function deleteCoupon(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/coupons/${id}`)
}

export function toggleCouponStatus(id: number, status: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/coupons/${id}/status`, { status })
}

// ===== Hot Keyword Management =====
export function getHotKeywordList(params: { page: number; pageSize: number; keyword?: string }): Promise<ApiResponse<PageResponse<HotKeyword>>> {
  return request.get('/admin/hot-keywords', { params })
}

export function createHotKeyword(data: { keyword: string; isPinned?: number; sortOrder?: number }): Promise<ApiResponse<HotKeyword>> {
  return request.post('/admin/hot-keywords', data)
}

export function updateHotKeyword(id: number, data: { keyword: string; isPinned?: number; sortOrder?: number }): Promise<ApiResponse<null>> {
  return request.put(`/admin/hot-keywords/${id}`, data)
}

export function deleteHotKeyword(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/hot-keywords/${id}`)
}

export function toggleHotKeywordPin(id: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/hot-keywords/${id}/pin`)
}

export function toggleHotKeywordStatus(id: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/hot-keywords/${id}/status`)
}

export function computeHotKeywords(days?: number, limit?: number): Promise<ApiResponse<null>> {
  return request.post('/admin/hot-keywords/compute', null, { params: { days, limit } })
}

// ===== Order Management =====
export function exportOrders(params: OrderQuery): Promise<Blob> {
  return request.get('/admin/orders/export', {
    params,
    responseType: 'blob',
  })
}

// ===== Product Import/Export =====
export function exportProducts(params: ProductQuery): Promise<Blob> {
  return request.get('/admin/products/export', {
    params,
    responseType: 'blob',
  })
}

export function importProducts(file: File): Promise<ApiResponse<{ successCount: number; failCount: number; totalCount: number; errors: { row: number; reason: string }[] }>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/products/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// ===== Seckill Management =====
export function getSeckillActivityList(params: { page: number; pageSize: number; keyword?: string; status?: number }): Promise<ApiResponse<PageResponse<SeckillActivity>>> {
  return request.get('/admin/seckill', { params })
}

export function getSeckillActivityById(id: number): Promise<ApiResponse<SeckillActivity>> {
  return request.get(`/admin/seckill/${id}`)
}

export function createSeckillActivity(data: SeckillActivityForm): Promise<ApiResponse<SeckillActivity>> {
  return request.post('/admin/seckill', data)
}

export function updateSeckillActivity(id: number, data: SeckillActivityForm): Promise<ApiResponse<null>> {
  return request.put(`/admin/seckill/${id}`, data)
}

export function deleteSeckillActivity(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/seckill/${id}`)
}

// ===== Feedback Management =====
export function getFeedbackList(params: { page: number; pageSize: number; type?: number; status?: number; keyword?: string }): Promise<ApiResponse<PageResponse<Feedback>>> {
  return request.get('/admin/feedbacks', { params })
}

export function getFeedbackDetail(id: number): Promise<ApiResponse<Feedback>> {
  return request.get(`/admin/feedbacks/${id}`)
}

export function replyFeedback(id: number, data: { status: number; adminReply: string }): Promise<ApiResponse<null>> {
  return request.put(`/admin/feedbacks/${id}/reply`, data)
}

export function deleteFeedback(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/feedbacks/${id}`)
}

// ===== Announcement Management =====
export function getAnnouncementList(params: PageQuery): Promise<ApiResponse<PageResponse<Announcement>>> {
  return request.get('/admin/announcements', { params })
}

export function getAnnouncementById(id: number): Promise<ApiResponse<Announcement>> {
  return request.get(`/admin/announcements/${id}`)
}

export function createAnnouncement(data: { title: string; content: string; status: number; sortOrder: number; level: string }): Promise<ApiResponse<Announcement>> {
  return request.post('/admin/announcements', data)
}

export function updateAnnouncement(id: number, data: { title: string; content: string; status: number; sortOrder: number; level: string }): Promise<ApiResponse<null>> {
  return request.put(`/admin/announcements/${id}`, data)
}

export function deleteAnnouncement(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/announcements/${id}`)
}

// ===== Review Management =====
export interface ReviewQuery {
  page: number
  pageSize: number
  keyword?: string
  username?: string
  rating?: number
  startDate?: string
  endDate?: string
  hasImage?: boolean
  hasFollowUp?: boolean
  sort?: string
}

export function getReviewList(params: ReviewQuery): Promise<ApiResponse<PageResponse<Review>>> {
  return request.get('/admin/reviews', { params })
}

export function getReviewDetail(id: number): Promise<ApiResponse<Review>> {
  return request.get(`/admin/reviews/${id}`)
}

export function deleteReview(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/reviews/${id}`)
}

export function batchDeleteReviews(ids: number[]): Promise<ApiResponse<null>> {
  return request.delete('/admin/reviews/batch', { data: { ids } })
}

// ===== Banner Management =====
export function getBannerList(params: PageQuery): Promise<ApiResponse<PageResponse<Banner>>> {
  return request.get('/admin/banners', { params })
}

export function getBannerById(id: number): Promise<ApiResponse<Banner>> {
  return request.get(`/admin/banners/${id}`)
}

export function createBanner(data: { title: string; imageUrl: string; linkUrl: string; sortOrder: number; status: number }): Promise<ApiResponse<Banner>> {
  return request.post('/admin/banners', data)
}

export function updateBanner(id: number, data: { title: string; imageUrl: string; linkUrl: string; sortOrder: number; status: number }): Promise<ApiResponse<null>> {
  return request.put(`/admin/banners/${id}`, data)
}

export function deleteBanner(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/banners/${id}`)
}

export function toggleBannerStatus(id: number, status: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/banners/${id}/status`, { status })
}
