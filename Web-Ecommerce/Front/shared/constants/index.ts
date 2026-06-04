import { OrderStatus, RefundStatus } from '../types'

export const ORDER_STATUS_MAP: Record<number, string> = {
  [OrderStatus.PENDING_PAY]: '待支付',
  [OrderStatus.PENDING_SHIP]: '待发货',
  [OrderStatus.SHIPPED]: '待收货',
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDING]: '退款中',
  [OrderStatus.REFUNDED]: '已退款',
}

export const ORDER_STATUS_COLOR: Record<number, string> = {
  [OrderStatus.PENDING_PAY]: 'warning',
  [OrderStatus.PENDING_SHIP]: 'primary',
  [OrderStatus.SHIPPED]: 'success',
  [OrderStatus.COMPLETED]: 'info',
  [OrderStatus.CANCELLED]: 'danger',
  [OrderStatus.REFUNDING]: 'warning',
  [OrderStatus.REFUNDED]: 'info',
}

export const REFUND_STATUS_MAP: Record<number, string> = {
  [RefundStatus.PENDING_REVIEW]: '待审核',
  [RefundStatus.REJECTED]: '已拒绝',
  [RefundStatus.COMPLETED]: '已完成',
  [RefundStatus.CANCELLED]: '已撤销',
}

export const REFUND_STATUS_COLOR: Record<number, string> = {
  [RefundStatus.PENDING_REVIEW]: 'warning',
  [RefundStatus.REJECTED]: 'danger',
  [RefundStatus.COMPLETED]: 'info',
  [RefundStatus.CANCELLED]: 'info',
}

export const REFUND_REASON_MAP: Record<string, string> = {
  dont_want: '不想要了',
  wrong_item: '买错了',
  not_as_described: '商品与描述不符',
  damaged: '商品破损',
  late_delivery: '未按约定时间发货',
  other: '其他',
}

export const FEEDBACK_TYPE_MAP: Record<number, string> = {
  1: '问题反馈',
  2: '功能建议',
}

export const FEEDBACK_STATUS_MAP: Record<number, string> = {
  0: '待处理',
  1: '处理中',
  2: '已解决',
  3: '已关闭',
}

export const FEEDBACK_STATUS_COLOR: Record<number, string> = {
  0: 'warning',
  1: 'primary',
  2: 'success',
  3: 'info',
}

export const DEFAULT_PAGE_SIZE = 20
export const ADMIN_PAGE_SIZE = 10

export const UPLOAD_MAX_SIZE = 2 * 1024 * 1024 // 2MB

export const TOKEN_KEY = 'ECOMMERCE_TOKEN'
export const USER_KEY = 'ECOMMERCE_USER'
export const REMEMBERED_USERNAME = 'ECOMMERCE_REMEMBERED_USERNAME'
export const ADMIN_TOKEN_KEY = 'ECOMMERCE_ADMIN_TOKEN'
export const ADMIN_USER_KEY = 'ECOMMERCE_ADMIN_USER'
