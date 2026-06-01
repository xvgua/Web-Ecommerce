export interface Coupon {
  id: number
  name: string
  type: number           // 1=满减券, 2=折扣券, 3=免邮券
  discount: number
  minAmount: number
  totalQty: number
  remainQty: number
  startTime: string
  endTime: string
  grabStartTime?: string  // 抢购开始时间
  grabEndTime?: string    // 抢购结束时间
  scopeType: number       // 1=通用 2=分类 3=单品
  scopeIds: string        // 适用范围ID（JSON数组）
  isLarge: number         // 0=小额 1=大额
  stackable: number       // 0=不可叠加 1=可叠加
  status: number
  received?: boolean
  userCouponStatus?: number  // null=未领取 0=未使用 1=已使用
  createTime: string
  scopeName?: string      // 适用范围描述
  grabStatus?: 'upcoming' | 'active' | 'ended' | 'none'
  grabCountdown?: number  // 距开始/结束的秒数
}

export interface UserCoupon {
  id: number
  userId: number
  couponId: number
  coupon?: Coupon
  status: number          // 0=未使用 1=已使用 2=已过期
  usedTime?: string
  useOrderId?: number
  createTime: string
}

export interface CouponForm {
  name: string
  type: number
  discount: number
  minAmount: number
  totalQty: number
  startTime: string
  endTime: string
  grabStartTime?: string
  grabEndTime?: string
  scopeType: number
  scopeIds: string
  isLarge: number
  stackable: number
  status: number
}
