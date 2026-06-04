import { OrderStatus } from '../types'

export const ORDER_STATUS_MAP: Record<number, string> = {
  [OrderStatus.PENDING_PAY]: '待支付',
  [OrderStatus.PENDING_SHIP]: '待发货',
  [OrderStatus.SHIPPED]: '待收货',
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDING]: '退款中',
}

export const ORDER_STATUS_COLOR: Record<number, string> = {
  [OrderStatus.PENDING_PAY]: 'warning',
  [OrderStatus.PENDING_SHIP]: 'primary',
  [OrderStatus.SHIPPED]: 'success',
  [OrderStatus.COMPLETED]: 'info',
  [OrderStatus.CANCELLED]: 'danger',
  [OrderStatus.REFUNDING]: 'warning',
}

export const DEFAULT_PAGE_SIZE = 20
export const ADMIN_PAGE_SIZE = 10

export const UPLOAD_MAX_SIZE = 2 * 1024 * 1024 // 2MB

export const TOKEN_KEY = 'ECOMMERCE_TOKEN'
export const USER_KEY = 'ECOMMERCE_USER'
export const REMEMBERED_USERNAME = 'ECOMMERCE_REMEMBERED_USERNAME'
export const ADMIN_TOKEN_KEY = 'ECOMMERCE_ADMIN_TOKEN'
export const ADMIN_USER_KEY = 'ECOMMERCE_ADMIN_USER'
