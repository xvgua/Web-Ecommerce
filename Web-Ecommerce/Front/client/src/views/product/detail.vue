<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-main" v-if="product">
      <div class="detail-gallery">
        <div class="detail-gallery__main">
          <ProductImage
            :src="product.mainImage"
            :seed="product.name + product.id"
            fit="cover"
          />
        </div>
        <div class="detail-gallery__thumbs" v-if="product.images?.length">
          <div
            v-for="(img, i) in product.images"
            :key="i"
            class="detail-gallery__thumb"
            :class="{ 'detail-gallery__thumb--active': activeImg === img }"
            @click="activeImg = img"
          >
            <ProductImage :src="img" :seed="`${product.id}-${i}`" fit="cover" />
          </div>
        </div>
      </div>

      <div class="detail-info">
        <div class="detail-info__category">
          <el-tag type="info" size="small">{{ product.categoryName }}</el-tag>
        </div>
        <h1 class="detail-info__name">{{ product.name }}</h1>
        <div class="detail-info__price-row">
          <span class="detail-info__price">{{ formatPrice(displayPrice) }}</span>
          <span class="detail-info__original" v-if="displayPrice < 99999">
            {{ formatPrice(displayPrice * 1.2) }}
          </span>
          <el-button
            class="favorite-btn"
            :class="{ 'favorite-btn--active': favorited }"
            :loading="favLoading"
            circle
            size="large"
            @click="handleToggleFavorite"
          >
            <el-icon size="20">
              <StarFilled v-if="favorited" />
              <Star v-else />
            </el-icon>
          </el-button>
        </div>

        <div class="detail-info__meta">
          <div class="meta-item">
            <el-icon><Box /></el-icon>
            <span>库存 {{ displayStock }} 件</span>
          </div>
          <div class="meta-item">
            <el-icon><TrendCharts /></el-icon>
            <span>已售 {{ product.sales }} 件</span>
          </div>
        </div>

        <div class="detail-info__divider" />

        <div class="detail-info__spec" v-if="product.skus?.length">
          <h4>商品规格</h4>
          <div class="sku-grid">
            <div
              v-for="sku in product.skus"
              :key="sku.id"
              :class="['sku-card', {
                'sku-card--active': selectedSkuId === sku.id,
                'sku-card--disabled': sku.stock === 0
              }]"
              @click="sku.stock > 0 && (selectedSkuId = sku.id)"
            >
              <div class="sku-card__img" v-if="sku.image">
                <img :src="sku.image" :alt="sku.specValue" />
              </div>
              <div class="sku-card__text">
                <span class="sku-card__name">{{ sku.specName }}</span>
              </div>
              <div class="sku-card__price" v-if="sku.price !== product.price">
                {{ formatPrice(sku.price) }}
              </div>
              <div class="sku-card__soldout" v-if="sku.stock === 0">已售罄</div>
            </div>
          </div>
        </div>

        <div class="detail-info__qty">
          <h4>数量</h4>
          <el-input-number
            v-model="quantity"
            :min="1"
            :max="displayStock"
            size="large"
          />
        </div>

        <div class="detail-info__actions">
          <el-button type="danger" size="large" :loading="addLoading" class="btn-add-cart" @click="handleAddToCart">
            <el-icon><ShoppingCart /></el-icon>
            加入购物车
          </el-button>
          <el-button type="primary" size="large" class="btn-buy-now" @click="handleBuyNow">
            立即购买
          </el-button>
        </div>
      </div>
    </div>

    <div class="detail-tabs" v-if="product">
      <el-tabs v-model="detailTab" class="detail-tabs__el">
        <el-tab-pane label="商品详情" name="desc">
          <div class="detail-tabs__body" v-html="product.description || '<p style=\'text-align:center;color:#999;padding:40px\'>暂无详细描述</p>'" />
        </el-tab-pane>
        <el-tab-pane label="详细参数" name="params">
          <div class="detail-tabs__body" v-html="product.detail || '<p style=\'text-align:center;color:#999;padding:40px\'>暂无详细参数</p>'" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ═══ 评价区域 ═══ -->
    <div class="review-section" v-if="product">
      <h2 class="review-section__title">
        商品评价
        <span class="review-section__count" v-if="product.reviewCount > 0">（{{ product.reviewCount }}）</span>
      </h2>

      <!-- 评分概览 + 分布 -->
      <div class="review-overview" v-if="reviewStats && reviewStats.reviewCount > 0">
        <div class="review-overview__score">
          <span class="review-overview__num">{{ reviewStats.avgRating }}</span>
          <el-rate
            :model-value="reviewStats.avgRating"
            disabled
            :max="5"
            :allow-half="true"
            class="review-overview__stars"
          />
          <span class="review-overview__total">{{ reviewStats.reviewCount }} 条评价</span>
        </div>
        <div class="review-overview__bars">
          <div
            v-for="star in 5"
            :key="star"
            class="review-bar"
            @click="ratingFilter = star"
            :class="{ 'review-bar--active': ratingFilter === star }"
          >
            <span class="review-bar__label">{{ star }} 星</span>
            <div class="review-bar__track">
              <div
                class="review-bar__fill"
                :style="{ width: barPercent(star) + '%' }"
              />
            </div>
            <span class="review-bar__count">{{ reviewStats.distribution[star] || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 评价筛选 -->
      <div class="review-filters" v-if="product.reviewCount > 0">
        <el-radio-group v-model="ratingFilter" size="small" @change="onFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="latest">最新</el-radio-button>
          <el-radio-button value="positive">好评</el-radio-button>
          <el-radio-button value="neutral">中评</el-radio-button>
          <el-radio-button value="negative">差评</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 评价列表 -->
      <div class="review-list" v-if="reviews.length > 0" v-loading="reviewLoading">
        <div class="review-item" v-for="r in reviews" :key="r.id">
          <div class="review-item__avatar">
            <el-avatar :size="40" :src="r.avatar || undefined">
              {{ maskName(r.username).charAt(0) }}
            </el-avatar>
          </div>
          <div class="review-item__body">
            <div class="review-item__header">
              <span class="review-item__user">{{ maskName(r.username) }}</span>
              <el-rate :model-value="r.rating" disabled :max="5" size="small" />
              <span class="review-item__time">{{ formatDate(r.createTime) }}</span>
            </div>
            <p class="review-item__content">{{ r.content }}</p>
            <div class="review-item__images" v-if="r.images?.length">
              <el-image
                v-for="(img, i) in r.images"
                :key="i"
                :src="img"
                fit="cover"
                :preview-src-list="r.images"
                :initial-index="i"
                class="review-item__img"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="review-pagination" v-if="reviewTotal > 0">
        <el-pagination
          v-model:current-page="reviewPage"
          :page-size="reviewPageSize"
          :total="reviewTotal"
          layout="total, prev, pager, next"
          @current-change="loadReviews"
        />
      </div>

      <el-empty v-if="!reviewLoading && reviews.length === 0" description="暂无评价" :image-size="80" />
    </div>

    <el-empty v-if="!loading && !product" description="商品不存在">
      <el-button type="primary" @click="$router.push('/products')">浏览其他商品</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, TrendCharts, ShoppingCart, Star, StarFilled } from '@element-plus/icons-vue'
import { getProductById, getProductReviews } from '@/api/product'
import { checkFavorite, addFavorite, removeFavorite } from '@/api/favorite'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { formatPrice } from '@/utils/format'
import type { Product, Review, ReviewRatingStats } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref<Product | null>(null)
const loading = ref(false)
const quantity = ref(1)
const addLoading = ref(false)
const activeImg = ref('')
const selectedSkuId = ref(0)
const detailTab = ref('desc')

const selectedSku = computed(() =>
  product.value?.skus?.find(s => s.id === selectedSkuId.value) || null
)
const displayPrice = computed(() =>
  selectedSku.value?.price ?? product.value?.price ?? 0
)
const displayStock = computed(() =>
  selectedSku.value?.stock ?? product.value?.stock ?? 0
)

// ── 评价状态 ──
const reviews = ref<Review[]>([])
const reviewStats = ref<ReviewRatingStats | null>(null)
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewPageSize = ref(10)
const reviewTotal = ref(0)
const ratingFilter = ref<string | number>('all')

// ── 收藏状态 ──
const favorited = ref(false)
const favLoading = ref(false)

async function checkFavStatus() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await checkFavorite(Number(route.params.id))
    favorited.value = res.data.favorited
  } catch { /* ignore */ }
}

async function handleToggleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // If product has SKUs, require selection before favoriting
  if (!favorited.value && product.value?.skus?.length && !selectedSkuId.value) {
    ElMessage.warning('请选择规格')
    return
  }
  favLoading.value = true
  try {
    if (favorited.value) {
      await removeFavorite(Number(route.params.id))
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(Number(route.params.id), selectedSkuId.value || undefined)
      favorited.value = true
      ElMessage.success('已收藏')
    }
  } finally {
    favLoading.value = false
  }
}

async function loadProduct() {
  loading.value = true
  try {
    const res = await getProductById(Number(route.params.id))
    product.value = res.data
    activeImg.value = product.value.mainImage
    if (product.value.skus?.length) {
      selectedSkuId.value = product.value.skus[0].id
    }
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  reviewLoading.value = true
  try {
    const range = filterToRange(ratingFilter.value)
    const res = await getProductReviews(
      Number(route.params.id), reviewPage.value, reviewPageSize.value,
      range.ratingMin, range.ratingMax
    )
    reviews.value = res.data.records.map(r => ({
      ...r,
      images: typeof r.images === 'string' ? JSON.parse(r.images || '[]') : (r.images || [])
    }))
    reviewTotal.value = res.data.total
    if (res.data.extra) {
      reviewStats.value = res.data.extra as unknown as ReviewRatingStats
    }
  } finally {
    reviewLoading.value = false
  }
}

function filterToRange(filter: string | number): { ratingMin?: number; ratingMax?: number } {
  if (typeof filter === 'number') {
    return { ratingMin: filter, ratingMax: filter }
  }
  switch (filter) {
    case 'positive': return { ratingMin: 4, ratingMax: 5 }
    case 'neutral':  return { ratingMin: 3, ratingMax: 3 }
    case 'negative': return { ratingMin: 1, ratingMax: 2 }
    default:         return {}
  }
}

function onFilterChange(val: string | number) {
  reviewPage.value = 1
  ratingFilter.value = val
  loadReviews()
}

function barPercent(star: number): number {
  if (!reviewStats.value || reviewStats.value.reviewCount === 0) return 0
  return ((reviewStats.value.distribution[star] || 0) / reviewStats.value.reviewCount) * 100
}

function maskName(name: string): string {
  if (!name || name.length <= 1) return name || '匿名'
  if (name.length === 2) return name[0] + '*'
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 30) return `${days} 天前`
  return dateStr.substring(0, 10)
}

async function handleAddToCart() {
  if (product.value?.skus?.length && !selectedSkuId.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  if (selectedSku.value && selectedSku.value.stock === 0) {
    ElMessage.warning('该规格已售罄')
    return
  }
  addLoading.value = true
  try {
    await cartStore.addItem(product.value!.id, selectedSkuId.value, quantity.value)
    ElMessage.success('已加入购物车')
  } finally {
    addLoading.value = false
  }
}

function handleBuyNow() {
  handleAddToCart()
  router.push('/cart')
}

onMounted(() => {
  loadProduct()
  loadReviews()
  checkFavStatus()
})
</script>

<style lang="scss" scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}

/* ── Main Section ── */
.detail-main {
  display: flex;
  gap: 48px;
  background: #fff;
  padding: 36px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.detail-gallery {
  width: 460px;
  flex-shrink: 0;

  &__main {
    aspect-ratio: 1;
    border-radius: 10px;
    overflow: hidden;
    background: #f8f8f8;
  }

  &__thumbs {
    display: flex;
    gap: 10px;
    margin-top: 12px;
  }

  &__thumb {
    width: 64px;
    height: 64px;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    transition: border-color .2s;

    &--active {
      border-color: #409eff;
    }
  }
}

/* ── Info ── */
.detail-info {
  flex: 1;
  min-width: 0;

  &__category { margin-bottom: 12px; }

  &__name {
    font-size: 24px;
    font-weight: 600;
    line-height: 1.4;
    margin-bottom: 16px;
    color: #1a1a1a;
  }

  &__price-row {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 20px;
  }

  &__price {
    font-size: 32px;
    font-weight: 700;
    color: #e6423a;
    font-family: 'SF Mono', monospace;
  }

  &__original {
    font-size: 16px;
    color: #c0c4cc;
    text-decoration: line-through;
  }

  .favorite-btn {
    margin-left: auto;
    border: 2px solid #eee;
    color: #999;
    transition: all .2s;

    &:hover {
      border-color: #f0a020;
      color: #f0a020;
    }

    &--active {
      border-color: #f0a020;
      color: #f0a020;
      background: rgba(240, 160, 32, .08);
    }
  }

  &__meta {
    display: flex;
    gap: 24px;
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: #666;
  }

  &__divider {
    height: 1px;
    background: #f0f0f0;
    margin: 20px 0;
  }

  &__spec h4,
  &__qty h4 {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 10px;
    color: #333;
  }

  &__qty {
    margin-top: 20px;
  }

  &__actions {
    display: flex;
    gap: 14px;
    margin-top: 28px;

    .btn-add-cart {
      flex: 1;
      height: 50px;
      font-size: 16px;
      border-radius: 10px;
      background: linear-gradient(135deg, #f2b5a2, #e89789);
      border: none;
      color: #5d3a30;

      &:hover {
        background: linear-gradient(135deg, #eba593, #e08878);
      }
    }

    .btn-buy-now {
      flex: 1;
      height: 50px;
      font-size: 16px;
      border-radius: 10px;
    }
  }
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.sku-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 8px;
  border: 2px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all .2s;
  position: relative;
  overflow: hidden;

  &:hover { border-color: #409eff; }

  &--active {
    border-color: #409eff;
    background: rgba(64, 158, 255, .08);
  }

  &--disabled {
    opacity: .45;
    cursor: not-allowed;

    &:hover { border-color: #eee; }
  }

  &__img {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 6px;
    overflow: hidden;
    margin-bottom: 8px;
    background: #f8f8f8;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__text {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: #333;
  }

  &__spec {
    font-size: 11px;
    color: #999;
  }

  &__price {
    font-size: 12px;
    color: #e6423a;
    font-family: 'SF Mono', monospace;
    margin-top: 4px;
  }

  &__soldout {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, .7);
    color: #999;
    font-size: 13px;
    font-weight: 600;
  }
}

@media (max-width: 1024px) {
  .sku-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .sku-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ── Detail Tabs ── */
.detail-tabs {
  background: #fff;
  padding: 32px;
  border-radius: 12px;
  margin-bottom: 24px;

  &__el {
    :deep(.el-tabs__header) {
      margin-bottom: 20px;
    }

    :deep(.el-tabs__item) {
      font-size: 16px;
      font-weight: 600;
    }
  }

  &__body {
    line-height: 1.8;
    color: #555;
    min-height: 120px;

    :deep(.param-table) {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;

      tr {
        &:nth-child(odd) {
          background: #fafafa;
        }
        &:hover {
          background: #f0f7ff;
        }
      }

      td {
        padding: 12px 16px;
        border-bottom: 1px solid #f0f0f0;

        &:first-child {
          width: 140px;
          color: #888;
          font-weight: 500;
          white-space: nowrap;
        }

        &:last-child {
          color: #333;
        }
      }
    }
  }
}

/* ── 评价区域 ── */
.review-section {
  background: #fff;
  padding: 32px;
  border-radius: 12px;
  margin-top: 24px;

  &__title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 2px solid #409eff;
    color: #1a1a1a;
  }

  &__count {
    font-size: 14px;
    font-weight: 400;
    color: #999;
  }
}

.review-overview {
  display: flex;
  gap: 48px;
  padding: 24px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;

  &__score {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 140px;
  }

  &__num {
    font-size: 48px;
    font-weight: 700;
    color: #e6423a;
    line-height: 1;
    font-family: 'SF Mono', monospace;
  }

  &__stars {
    margin: 8px 0;
  }

  &__total {
    font-size: 13px;
    color: #999;
  }

  &__bars {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
}

.review-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: background .2s;

  &:hover { background: #f5f7fa; }

  &--active {
    background: rgba(64,158,255,.08);
    .review-bar__label { color: #409eff; font-weight: 600; }
  }

  &__label {
    width: 36px;
    color: #666;
    flex-shrink: 0;
  }

  &__track {
    flex: 1;
    height: 8px;
    background: #f0f0f0;
    border-radius: 4px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: #f7ba2a;
    border-radius: 4px;
    transition: width .4s;
  }

  &__count {
    width: 30px;
    color: #999;
    text-align: right;
    flex-shrink: 0;
  }
}

.review-filters {
  margin-bottom: 20px;
}

.review-list {
  min-height: 120px;
}

.review-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }

  &__avatar { flex-shrink: 0; }

  &__body { flex: 1; min-width: 0; }

  &__header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
    flex-wrap: wrap;
  }

  &__user {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  &__time {
    font-size: 12px;
    color: #c0c4cc;
    margin-left: auto;
  }

  &__content {
    font-size: 14px;
    line-height: 1.7;
    color: #555;
    margin-bottom: 8px;
    word-break: break-word;
  }

  &__images {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__img {
    width: 80px;
    height: 80px;
    border-radius: 6px;
    border: 1px solid #eee;
    cursor: pointer;
  }
}

.review-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 1024px) {
  .detail-main {
    gap: 32px;
    padding: 24px;
  }
  .detail-gallery {
    width: 360px;
  }
}

@media (max-width: 768px) {
  .detail-main {
    flex-direction: column;
    gap: 24px;
    padding: 16px;
  }
  .detail-gallery {
    width: 100%;
  }
  .detail-info {
    &__name { font-size: 20px; }
    &__price { font-size: 26px; }
    &__actions {
      flex-direction: column;
    }
  }
}
</style>
