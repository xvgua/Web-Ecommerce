import type { Address } from './user'

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  skuId: number
  specDesc: string
  quantity: number
  price: number
  isReviewed?: boolean
  hasFollowUp?: boolean
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  addressId: number
  address: Address
  totalAmount: number
  status: number
  statusText: string
  payMethod?: string
  payTime: string
  shipTime?: string
  dealTime?: string
  createTime: string
  updateTime?: string
  addressModified?: number
  reviewCount?: number
  items: OrderItem[]
}

export interface OrderQuery extends PageQuery {
  status?: number
  reviewFilter?: string  // 'pending' | 'followup' | 'reviewed'
}

export interface CreateOrderForm {
  addressId: number
  cartItemIds: number[]
  remark: string
}

export interface PaymentForm {
  orderId: number
  payMethod: string
}

export interface PayIntent {
  qrToken: string
  orderNo: string
  amount: number
  payMethod: string
}

export interface PayStatus {
  status: 'WAITING_SCAN' | 'SCANNED' | 'PAID' | 'NONE'
  scanned: boolean
  payMethod: string | null
}
