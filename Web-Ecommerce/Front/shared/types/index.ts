// ========== API 通用类型 ==========

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  extra?: Record<string, unknown>
}

export interface PageQuery {
  page: number
  pageSize: number
  keyword?: string
}

// ========== 通用枚举 ==========

export enum OrderStatus {
  PENDING_PAY = 0,
  PENDING_SHIP = 1,
  SHIPPED = 2,
  COMPLETED = 3,
  CANCELLED = 4,
  REFUNDING = 5,
  REFUNDED = 6,
}

export enum RefundStatus {
  PENDING_REVIEW = 0,
  REJECTED = 1,
  COMPLETED = 2,
  CANCELLED = 3,
}

export enum RefundType {
  ONLY_REFUND = 1,
  RETURN_REFUND = 2,
}

export enum UserStatus {
  ACTIVE = 1,
  DISABLED = 0,
}

export enum ProductStatus {
  ON_SALE = 1,
  OFF_SHELF = 0,
}

export enum FeedbackType {
  BUG_REPORT = 1,
  SUGGESTION = 2,
}

export enum FeedbackStatus {
  PENDING = 0,
  PROCESSING = 1,
  RESOLVED = 2,
  CLOSED = 3,
}

export enum AnnouncementStatus {
  DRAFT = 0,
  PUBLISHED = 1,
  ARCHIVED = 2,
}

export enum AnnouncementLevel {
  INFO = 'info',
  WARNING = 'warning',
  IMPORTANT = 'important',
}

export interface Announcement {
  id: number
  title: string
  content: string
  status: number
  sortOrder: number
  level: string
  createTime: string
  updateTime: string
}

export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
  status: number
  createTime: string
  updateTime: string
}
