import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Product, ProductQuery, Category, ProductForm } from '@shared/types/product'
import type { Order, OrderQuery } from '@shared/types/order'
import type { User } from '@shared/types/user'

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
}>> {
  return request.get('/admin/dashboard/stats')
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
