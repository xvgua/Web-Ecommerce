export interface Category {
  id: number
  name: string
  parentId: number
  sortOrder: number
  children?: Category[]
  createTime?: string
}

export interface Product {
  id: number
  name: string
  categoryId: number
  categoryName: string
  price: number
  stock: number
  description: string
  mainImage: string
  images: string[]
  status: number
  sales: number
  avgRating: number
  reviewCount: number
  createTime: string
}

export interface ProductSku {
  id: number
  productId: number
  specName: string
  specValue: string
  price: number
  stock: number
}

export interface ProductQuery extends PageQuery {
  categoryId?: number
  sort?: 'price_asc' | 'price_desc' | 'sales_desc' | 'newest'
  minPrice?: number
  maxPrice?: number
}

export interface ProductForm {
  name: string
  categoryId: number
  price: number
  stock: number
  description: string
  mainImage: string
  images: string[]
  status: number
}

export interface Review {
  id: number
  userId: number
  username: string
  avatar: string
  productId: number
  orderId: number
  rating: number
  content: string
  images: string[]
  createTime: string
}

export interface ReviewRatingStats {
  avgRating: number
  reviewCount: number
  distribution: Record<number, number>
}
