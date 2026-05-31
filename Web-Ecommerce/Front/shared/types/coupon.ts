export interface Coupon {
  id: number
  name: string
  type: number        // 1=满减券, 2=折扣券, 3=免邮券
  discount: number
  minAmount: number
  totalQty: number
  remainQty: number
  startTime: string
  endTime: string
  status: number
  received?: boolean
  createTime: string
}
