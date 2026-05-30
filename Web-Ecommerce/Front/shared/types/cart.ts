export interface CartItem {
  id: number
  userId: number
  productId: number
  productName: string
  productImage: string
  skuId: number
  specDesc: string
  hasSku: boolean
  price: number
  quantity: number
  stock: number
  checked: boolean
}

export interface AddToCartForm {
  productId: number
  skuId: number
  quantity: number
}

export interface UpdateCartForm {
  id: number
  quantity: number
  checked?: boolean
  skuId?: number
}
