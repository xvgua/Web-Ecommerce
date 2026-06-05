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
  detail: string
  mainImage: string
  images: string[]
  status: number
  sales: number
  avgRating: number
  reviewCount: number
  listedAt?: string
  createTime: string
  skus?: ProductSku[]
}

export interface ProductSku {
  id: number
  productId: number
  specName: string
  specValue?: string
  price: number
  stock: number
  sales: number
  status: number
  image?: string
}

export interface SkuForm {
  specName: string
  specValue?: string
  price: number
  stock: number
  status?: number
  image?: string
}

export interface ProductQuery extends PageQuery {
  categoryId?: number
  status?: number
  sort?: 'price_asc' | 'price_desc' | 'sales_desc' | 'newest' | 'rating_desc' | 'rating_asc'
  minPrice?: number
  maxPrice?: number
}

export interface ProductForm {
  name: string
  categoryId: number
  price: number
  stock: number
  description: string
  detail: string
  mainImage: string
  images: string[]
  status: number
  skus?: SkuForm[]
}

export interface Review {
  id: number
  userId: number
  username: string
  avatar: string
  productId: number
  orderId: number
  rating: number
  ratingDesc?: number
  ratingLogistics?: number
  ratingService?: number
  content: string
  images: string[]
  isFollowup?: number
  likeCount?: number
  commentCount?: number
  isLiked?: boolean
  hasFollowUp?: boolean
  followUpReviews?: Review[]
  comments?: ReviewComment[]
  productName?: string
  productImage?: string
  productPrice?: number
  createTime: string
}

export interface ReviewComment {
  id: number
  reviewId: number
  userId: number
  username: string
  avatar: string
  content: string
  createTime: string
}

export interface ReviewRatingStats {
  avgRating: number
  reviewCount: number
  distribution: Record<number, number>
}

export interface HotKeyword {
  id: number
  keyword: string
  searchCount: number
  isManual: number
  isPinned: number
  sortOrder: number
  status: number
}
