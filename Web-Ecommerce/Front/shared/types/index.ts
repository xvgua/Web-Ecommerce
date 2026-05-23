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
}

export enum UserStatus {
  ACTIVE = 1,
  DISABLED = 0,
}

export enum ProductStatus {
  ON_SALE = 1,
  OFF_SHELF = 0,
}
