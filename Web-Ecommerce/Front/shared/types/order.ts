import type { Address } from './user'
import type { PageQuery } from './index'

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
  couponIds?: string
  couponDiscount?: number
  discountAmount?: number
  payAmount?: number
  couponName?: string
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
  // Refund fields
  refundType?: number
  refundReason?: string
  refundReasonText?: string
  refundDesc?: string
  refundAmount?: number
  refundItemIds?: string
  refundStatus?: number
  refundStatusText?: string
  refundRejectReason?: string
  refundApplyTime?: string
  refundDealTime?: string
  refundItems?: OrderItem[]
}

export interface OrderQuery extends PageQuery {
  status?: number
  userId?: number
  reviewFilter?: string  // 'pending' | 'followup' | 'reviewed'
  hasRefund?: boolean
}

export interface CreateOrderForm {
  addressId: number
  cartItemIds?: number[]
  productId?: number
  skuId?: number
  quantity?: number
  remark?: string
  userCouponIds?: number[]
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
