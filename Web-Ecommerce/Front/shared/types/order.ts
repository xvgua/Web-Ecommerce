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
  payTime: string
  createTime: string
  items: OrderItem[]
}

export interface OrderQuery extends PageQuery {
  status?: number
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
