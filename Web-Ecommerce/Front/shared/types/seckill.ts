export interface SeckillActivity {
  id: number
  name: string
  startTime: string
  endTime: string
  status: number        // 0=未开始 1=进行中 2=已结束
  products?: SeckillProduct[]
  createTime: string
  updateTime: string
}

export interface SeckillProduct {
  id: number
  activityId: number
  productId: number
  skuId: number
  seckillPrice: number
  seckillStock: number
  remainStock: number
  limitPerUser: number
  productName?: string
  productImage?: string
  originalPrice?: number
  specDesc?: string
  createTime: string
}

export interface SeckillProductForm {
  productId: number
  skuId?: number
  seckillPrice: number
  seckillStock: number
  limitPerUser?: number
}

export interface SeckillActivityForm {
  name: string
  startTime: string
  endTime: string
  products: SeckillProductForm[]
}

export interface SeckillOrderRequest {
  seckillProductId: number
  addressId: number
  userCouponIds?: number[]
  remark?: string
}
