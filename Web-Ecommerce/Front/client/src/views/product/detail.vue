<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-hero" v-if="product">
      <!-- ═══ Gallery: vertical thumbs + main image ═══ -->
      <div class="detail-gallery">
        <div class="gallery-body">
          <div class="gallery-thumbs" v-if="allImages.length">
            <div
              v-for="(img, i) in allImages"
              :key="i"
              class="gallery-thumb"
              :class="{ 'gallery-thumb--active': activeImg === img }"
              @click="activeImg = img"
            >
              <ProductImage :src="img" :seed="`${product.id}-${i}`" fit="cover" />
            </div>
          </div>

          <div class="gallery-main">
            <ProductImage :src="activeImg" :seed="product.name + product.id" fit="cover" />
          </div>
        </div>

        <!-- Switch tabs: 图集 / 参数 -->
        <div class="gallery-tabs">
          <span
            class="gallery-tab"
            :class="{ 'gallery-tab--active': galleryTab === 'images' }"
            @click="galleryTab = 'images'"
          >图集</span>
          <span
            class="gallery-tab"
            :class="{ 'gallery-tab--active': galleryTab === 'params' }"
            @click="galleryTab = 'params'"
          >参数</span>
        </div>

        <div
          v-if="galleryTab === 'params'"
          class="gallery-params"
          v-html="product.detail || noParamsHtml"
        />
      </div>

      <!-- ═══ Info: price + SKU + actions ═══ -->
      <div class="detail-info">
        <div class="detail-info__category">
          <el-tag type="info" size="small">{{ product.categoryName }}</el-tag>
        </div>
        <h1 class="detail-info__name">{{ product.name }}</h1>

        <div class="detail-info__price-section">
          <!-- Seckill price -->
          <div class="price-row price-row--seckill" v-if="seckillInfo">
            <span class="price-label price-label--seckill">秒杀价</span>
            <span class="price-symbol">¥</span>
            <span class="price-value price-value--seckill">{{ seckillInfo.seckillPrice }}</span>
          </div>
          <!-- Normal price (no seckill) -->
          <div class="price-row price-row--normal" v-else>
            <span class="price-label" v-if="showPriceLabel">价格</span>
            <span class="price-symbol">¥</span>
            <span class="price-value">{{ displayPrice }}</span>
            <span class="price-suffix" v-if="showPriceSuffix">起</span>
          </div>
          <!-- Original price -->
          <div class="price-row price-row--original" v-if="showOriginalPrice">
            <span class="price-label">原价</span>
            <span class="price-symbol">¥</span>
            <span class="price-value price-value--original">{{ originalPriceValue }}</span>
            <span class="price-suffix" v-if="showPriceSuffix">起</span>
          </div>
          <!-- Seckill countdown -->
          <div class="price-row price-row--countdown" v-if="seckillInfo && seckillCountdown">
            <span class="countdown-text">距结束 {{ seckillCountdown }}</span>
          </div>
          <!-- Favorite button -->
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

        <div class="detail-info__sales-data">
          <span>已售 {{ displaySales }}+</span>
          <span class="sales-sep">|</span>
          <span>{{ product.reviewCount }} 条评价</span>
          <span class="sales-sep">|</span>
          <span>好评率 {{ product.avgRating ?? '-' }}%</span>
        </div>

        <div class="detail-info__meta">
          <div class="meta-item">
            <el-icon><Box /></el-icon>
            <span>库存 {{ displayStock }} 件</span>
          </div>
        </div>

        <div class="detail-info__divider" />

        <!-- SKU selector: grouped by specName -->
        <div class="detail-info__spec" v-if="product.skus?.length">
          <div v-for="group in skuGroups" :key="group.name" class="sku-group">
            <h4 class="sku-group__label">{{ group.name }}</h4>
            <div class="sku-group__chips">
              <div
                v-for="sku in group.values"
                :key="sku.id"
                :class="['sku-chip', {
                  'sku-chip--active': selectedSkuId === sku.id,
                  'sku-chip--disabled': sku.stock === 0
                }]"
                @click="selectSku(sku)"
              >
                <img v-if="sku.image" :src="sku.image" class="sku-chip__img" />
                <span class="sku-chip__text">{{ sku.specValue || sku.specName }}</span>
                <span
                  v-if="sku.price !== product.price"
                  class="sku-chip__diff"
                >{{ sku.price > product.price ? '+' : '' }}{{ formatPrice(sku.price - product.price) }}</span>
                <span v-if="sku.stock === 0" class="sku-chip__soldout">售罄</span>
              </div>
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
          <el-button size="large" :icon="ChatDotSquare" class="btn-chat" @click="openProductChat">
            咨询客服
          </el-button>
        </div>
      </div>
    </div>

    <!-- ═══ Sticky Nav ═══ -->
    <div class="detail-sticky-nav" ref="stickyNavRef" v-if="product">
      <nav class="sticky-nav__tabs">
        <a
          v-for="tab in stickyTabs"
          :key="tab.id"
          class="sticky-nav__tab"
          :class="{ 'sticky-nav__tab--active': sticky.activeTab === tab.id }"
          @click.prevent="sticky.scrollToSection(tab.id)"
        >{{ tab.label }}</a>
      </nav>
    </div>

    <!-- ═══ Description Section ═══ -->
    <section id="detail-description" class="detail-section" v-if="product">
      <h2 class="detail-section__title">商品详情</h2>
      <div class="detail-section__body" v-html="product.description || noDescHtml" />
    </section>

    <!-- ═══ Params Section ═══ -->
    <section id="detail-params" class="detail-section" v-if="product">
      <h2 class="detail-section__title">详细参数</h2>
      <div class="detail-section__body" v-html="product.detail || noParamsHtml" />
    </section>

    <!-- ═══ Reviews Section ═══ -->
    <section id="detail-reviews" class="detail-section review-section" v-if="product">
      <h2 class="detail-section__title">
        用户评价
        <span class="detail-section__count" v-if="product.reviewCount > 0"> · {{ product.reviewCount }}</span>
        <span class="detail-section__rate" v-if="reviewStats && reviewStats.avgRating">好评率 {{ reviewStats.avgRating }}%</span>
      </h2>

      <!-- Rating overview -->
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

      <!-- Filter -->
      <div class="review-filters" v-if="product.reviewCount > 0">
        <el-radio-group v-model="ratingFilter" size="small" @change="onFilterChange">
          <el-radio-button value="all">全部（{{ reviewStats?.reviewCount ?? 0 }}）</el-radio-button>
          <el-radio-button value="latest">最新</el-radio-button>
          <el-radio-button value="positive">好评（{{ filterCount('positive') }}）</el-radio-button>
          <el-radio-button value="neutral">中评（{{ filterCount('neutral') }}）</el-radio-button>
          <el-radio-button value="negative">差评（{{ filterCount('negative') }}）</el-radio-button>
        </el-radio-group>
      </div>

      <!-- Review list -->
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
    </section>

    <el-empty v-if="!loading && !product" description="商品不存在">
      <el-button type="primary" @click="$router.push('/products')">浏览其他商品</el-button>
    </el-empty>

    <ChatDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, ShoppingCart, Star, StarFilled, ChatDotSquare } from '@element-plus/icons-vue'
import { getProductById, getProductReviews } from '@/api/product'
import { checkFavorite, addFavorite, removeFavorite } from '@/api/favorite'
import { getActiveActivities } from '@/api/seckill'
import type { SeckillProduct } from '@shared/types/seckill'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { formatPrice } from '@/utils/format'
import { useStickyTabs } from '@/composables/useStickyTabs'
import type { StickyTab } from '@/composables/useStickyTabs'
import type { Product, ProductSku, Review, ReviewRatingStats } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'
import ChatDialog from '@/components/business/ChatDialog.vue'

const chatStore = useChatStore()
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
const galleryTab = ref<'images' | 'params'>('images')

// ── Seckill ──
const seckillInfo = ref<SeckillProduct | null>(null)
const seckillCountdown = ref('')
let seckillTimer: ReturnType<typeof setInterval> | null = null

// ── Placeholders ──
const noDescHtml = '<p style="text-align:center;color:#999;padding:40px">暂无详细描述</p>'
const noParamsHtml = '<p style="text-align:center;color:#999;padding:40px">暂无详细参数</p>'

const selectedSku = computed(() =>
  product.value?.skus?.find(s => s.id === selectedSkuId.value) || null
)
const displayPrice = computed(() =>
  selectedSku.value?.price ?? product.value?.price ?? 0
)
const displayStock = computed(() =>
  selectedSku.value?.stock ?? product.value?.stock ?? 0
)
const displaySales = computed(() =>
  selectedSku.value?.sales ?? product.value?.sales ?? 0
)

const allImages = computed(() => {
  if (!product.value) return []
  const seen = new Set<string>()
  const result: string[] = []
  // main image first
  if (product.value.mainImage) {
    seen.add(product.value.mainImage)
    result.push(product.value.mainImage)
  }
  // product detail images
  for (const img of product.value.images || []) {
    if (!seen.has(img)) { seen.add(img); result.push(img) }
  }
  // SKU images
  for (const sku of product.value.skus || []) {
    if (sku.image && !seen.has(sku.image)) {
      seen.add(sku.image)
      result.push(sku.image)
    }
  }
  return result
})

interface SkuGroup { name: string; values: ProductSku[] }
const skuGroups = computed<SkuGroup[]>(() => {
  if (!product.value?.skus) return []
  const map = new Map<string, ProductSku[]>()
  for (const sku of product.value.skus) {
    const arr = map.get(sku.specName)
    if (arr) {
      arr.push(sku)
    } else {
      map.set(sku.specName, [sku])
    }
  }
  return Array.from(map.entries()).map(([name, values]) => ({ name, values }))
})

function selectSku(sku: ProductSku) {
  if (sku.stock === 0) return
  selectedSkuId.value = sku.id
  quantity.value = 1
  if (sku.image) activeImg.value = sku.image
}

// ── Price display helpers ──
const showOriginalPrice = computed(() => {
  if (seckillInfo.value?.originalPrice) return true
  if (product.value?.skus?.length && displayPrice.value < product.value.price) return true
  return false
})

const originalPriceValue = computed(() => {
  if (seckillInfo.value?.originalPrice) return seckillInfo.value.originalPrice
  return product.value?.price ?? 0
})

const showPriceLabel = computed(() => {
  return !!(product.value?.skus?.length && displayPrice.value < product.value.price)
})

const showPriceSuffix = computed(() => {
  if (!product.value?.skus?.length) return false
  if (selectedSkuId.value) return false
  return true
})

// ── Seckill check ──
async function checkSeckill() {
  if (!product.value) return
  try {
    const res = await getActiveActivities()
    const activities = res.data || []
    for (const act of activities) {
      if (act.status !== 1) continue
      const sp = act.products?.find(p => p.productId === product.value!.id)
      if (sp && sp.remainStock > 0) {
        seckillInfo.value = sp
        updateSeckillCountdown(act.endTime)
        seckillTimer = setInterval(() => updateSeckillCountdown(act.endTime), 1000)
        return
      }
    }
  } catch { /* seckill unavailable */ }
}

function updateSeckillCountdown(endTime: string) {
  const diff = new Date(endTime).getTime() - Date.now()
  if (diff <= 0) {
    seckillInfo.value = null
    seckillCountdown.value = ''
    if (seckillTimer) { clearInterval(seckillTimer); seckillTimer = null }
    return
  }
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  seckillCountdown.value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// ── Review state ──
const reviews = ref<Review[]>([])
const reviewStats = ref<ReviewRatingStats | null>(null)
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewPageSize = ref(10)
const reviewTotal = ref(0)
const ratingFilter = ref<string | number>('all')

// ── Favorite state ──
const favorited = ref(false)
const favLoading = ref(false)

// ── Sticky tabs ──
const stickyTabs: StickyTab[] = [
  { id: 'detail-description', label: '详情' },
  { id: 'detail-params', label: '参数' },
  { id: 'detail-reviews', label: '评价' },
]
const stickyNavRef = ref<HTMLElement | null>(null)
const sticky = useStickyTabs(stickyTabs, stickyNavRef)

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

function filterCount(filter: string): number {
  if (!reviewStats.value) return 0
  const d = reviewStats.value.distribution
  switch (filter) {
    case 'positive': return (d[4] || 0) + (d[5] || 0)
    case 'neutral':  return d[3] || 0
    case 'negative': return (d[1] || 0) + (d[2] || 0)
    default:         return reviewStats.value.reviewCount
  }
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

function openProductChat() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!product.value) return
  const selected = selectedSku.value
  chatStore.openChat({
    productId: product.value.id,
    productName: product.value.name,
    productImage: product.value.mainImage || '',
    price: displayPrice.value,
    specDesc: selected ? `${selected.specName}:${selected.specValue}` : undefined,
  })
}

async function handleBuyNow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (product.value?.skus?.length && !selectedSkuId.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  if (selectedSku.value && selectedSku.value.stock === 0) {
    ElMessage.warning('该规格已售罄')
    return
  }
  router.push({
    path: '/order/confirm',
    query: {
      productId: product.value!.id,
      skuId: selectedSkuId.value || 0,
      quantity: quantity.value,
    },
  })
}

onMounted(() => {
  loadProduct().then(() => checkSeckill())
  loadReviews()
  checkFavStatus()
  sticky.setupObserver()
})

onUnmounted(() => {
  if (seckillTimer) { clearInterval(seckillTimer); seckillTimer = null }
})
</script>

<style lang="scss" scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}

/* ═══════════════════════════════════════════
   HERO: Gallery + Info
   ═══════════════════════════════════════════ */
.detail-hero {
  display: flex;
  gap: 48px;
  background: var(--bg1);
  padding: 36px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  border: 1px solid var(--line-light);
}

/* ── Gallery ── */
.detail-gallery {
  width: 520px;
  flex-shrink: 0;
  overflow: hidden;
}

.gallery-body {
  display: flex;
  gap: 12px;
  position: relative;
}

.gallery-thumbs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 72px;
  flex-shrink: 0;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: var(--line-regular); border-radius: 2px; }
}

.gallery-thumb {
  width: 68px;
  height: 68px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color var(--transition-fast);
  flex-shrink: 0;

  &:hover { border-color: var(--line-regular); }

  &--active {
    border-color: var(--brand-primary);
  }

}

.gallery-main {
  width: 420px;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg2);
}

.gallery-tabs {
  display: flex;
  gap: 0;
  margin-top: 12px;
  border-bottom: 2px solid var(--line-light);
}

.gallery-tab {
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text3);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all var(--transition-fast);

  &:hover { color: var(--text1); }

  &--active {
    color: var(--brand-primary);
    border-bottom-color: var(--brand-primary);
  }
}

.gallery-params {
  padding: 16px 0;
  line-height: 1.8;
  color: var(--text2);
  min-height: 120px;

  :deep(.param-table) {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;

    tr {
      &:nth-child(odd) { background: var(--bg2); }
      &:hover { background: var(--brand-primary-ghost); }
    }

    td {
      padding: 12px 16px;
      border-bottom: 1px solid var(--line-light);

      &:first-child {
        width: 140px;
        color: var(--text3);
        font-weight: 500;
        white-space: nowrap;
      }

      &:last-child { color: var(--text1); }
    }
  }
}

/* ── Info ── */
.detail-info {
  flex: 1;
  min-width: 0;

  &__category { margin-bottom: 12px; }

  &__name {
    font-size: 22px;
    font-weight: 600;
    line-height: 1.4;
    margin-bottom: 16px;
    color: var(--text1);
  }

  &__price-section {
    position: relative;
    margin-bottom: 10px;
  }

  .price-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: 4px;

    &--countdown { margin-bottom: 0; }
  }

  .price-label {
    font-size: 13px;
    font-weight: 400;
    color: var(--text3);
    margin-right: 4px;

    &--seckill {
      font-size: 12px;
      font-weight: 500;
      color: #fff;
      background: #e6423a;
      border-radius: 3px;
      padding: 1px 8px;
    }
  }

  .price-symbol {
    font-size: 16px;
    font-weight: 600;
    color: #e6423a;
  }

  .price-value {
    font-size: 28px;
    font-weight: 700;
    color: #e6423a;
    letter-spacing: -0.5px;

    &--seckill { font-size: 30px; }

    &--original {
      font-size: 16px;
      font-weight: 400;
      color: var(--text4);
      text-decoration: line-through;
    }
  }

  .price-suffix {
    font-size: 12px;
    font-weight: 400;
    color: inherit;
    margin-left: 2px;
  }

  .price-row--original {
    .price-symbol, .price-suffix { color: var(--text4); }
  }

  .countdown-text {
    font-size: 13px;
    font-weight: 500;
    color: #e6423a;
    font-variant-numeric: tabular-nums;
  }

  .favorite-btn {
    position: absolute;
    top: 0;
    right: 0;
    margin-left: auto;
    border: 2px solid var(--line-light);
    color: var(--text3);
    transition: all var(--transition-fast);

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

  &__sales-data {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: var(--text3);
    margin-bottom: 16px;

    .sales-sep {
      width: 1px;
      height: 10px;
      background: var(--line-light);
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
    color: var(--text2);
  }

  &__divider {
    height: 1px;
    background: var(--line-light);
    margin: 16px 0;
  }

  &__spec {
    margin-bottom: 16px;
  }

  &__qty {
    h4 {
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
      color: var(--text1);
    }
  }

  &__actions {
    display: flex;
    gap: 14px;
    margin-top: 24px;

    .btn-add-cart {
      flex: 1;
      height: 48px;
      font-size: 16px;
      border-radius: var(--radius-sm);
      background: linear-gradient(135deg, #f2b5a2, #e89789);
      border: none;
      color: #5d3a30;

      &:hover {
        background: linear-gradient(135deg, #eba593, #e08878);
      }
    }

    .btn-buy-now {
      flex: 1;
      height: 48px;
      font-size: 16px;
      border-radius: var(--radius-sm);
    }

    .btn-chat {
      flex-shrink: 0;
    }
  }
}

/* ── SKU Chips ── */
.sku-group {
  margin-bottom: 14px;

  &:last-child { margin-bottom: 0; }
}

.sku-group__label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text1);
}

.sku-group__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sku-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 1px solid var(--line-regular);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  background: var(--bg1);

  &:hover { border-color: var(--brand-primary); }

  &--active {
    border-color: var(--brand-primary);
    background: var(--brand-primary-light);
    color: var(--brand-primary);
  }

  &--disabled {
    opacity: 0.4;
    cursor: not-allowed;

    &:hover { border-color: var(--line-regular); }
  }

  &__img {
    width: 36px;
    height: 36px;
    border-radius: 2px;
    object-fit: cover;
    flex-shrink: 0;
  }

  &__text {
    font-size: 13px;
    color: var(--text1);
    white-space: nowrap;

    .sku-chip--active & { color: var(--brand-primary); }
  }

  &__diff {
    font-size: 11px;
    color: #e6423a;
    white-space: nowrap;
  }

  &__soldout {
    position: absolute;
    top: -1px;
    right: -1px;
    background: var(--bg3);
    color: var(--text4);
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 0 var(--radius-sm) 0 var(--radius-sm);
  }
}

/* ═══════════════════════════════════════════
   STICKY NAV
   ═══════════════════════════════════════════ */
.detail-sticky-nav {
  position: sticky;
  top: 56px;
  z-index: 40;
  background: var(--bg1);
  border-bottom: 1px solid var(--line-light);
  max-width: 52.5%;
  margin: 0 auto;
  padding: 0 24px;
}

.sticky-nav__tabs {
  display: flex;
  gap: 0;
  width: 100%;
}

.sticky-nav__tab {
  display: inline-block;
  padding: 14px 24px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text3);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover { color: var(--text1); }

  &--active {
    color: var(--brand-primary);
    border-bottom-color: var(--brand-primary);
  }
}

/* ═══════════════════════════════════════════
   SECTIONS
   ═══════════════════════════════════════════ */
.detail-section {
  background: var(--bg1);
  padding: 32px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  border: 1px solid var(--line-light);

  &__title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 2px solid var(--brand-primary);
    color: var(--text1);
  }

  &__count {
    font-size: 14px;
    font-weight: 400;
    color: var(--text4);
  }

  &__rate {
    font-size: 13px;
    font-weight: 400;
    color: var(--text3);
    margin-left: 16px;
  }

  &__body {
    line-height: 1.8;
    color: var(--text2);
    min-height: 120px;

    :deep(.param-table) {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;

      tr {
        &:nth-child(odd) { background: var(--bg2); }
        &:hover { background: var(--brand-primary-ghost); }
      }

      td {
        padding: 12px 16px;
        border-bottom: 1px solid var(--line-light);

        &:first-child {
          width: 140px;
          color: var(--text3);
          font-weight: 500;
          white-space: nowrap;
        }

        &:last-child { color: var(--text1); }
      }
    }
  }
}

/* ═══════════════════════════════════════════
   REVIEWS
   ═══════════════════════════════════════════ */
.review-overview {
  display: flex;
  gap: 48px;
  padding: 24px 0;
  border-bottom: 1px solid var(--line-light);
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
  }

  &__stars { margin: 8px 0; }

  &__total {
    font-size: 13px;
    color: var(--text4);
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
  transition: background var(--transition-fast);

  &:hover { background: var(--bg2); }

  &--active {
    background: var(--brand-primary-light);
    .review-bar__label { color: var(--brand-primary); font-weight: 600; }
  }

  &__label { width: 36px; color: var(--text2); flex-shrink: 0; }

  &__track {
    flex: 1;
    height: 8px;
    background: var(--bg3);
    border-radius: 4px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: #f7ba2a;
    border-radius: 4px;
    transition: width 0.4s;
  }

  &__count { width: 30px; color: var(--text4); text-align: right; flex-shrink: 0; }
}

.review-filters { margin-bottom: 20px; }

.review-list { min-height: 120px; }

.review-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid var(--line-light);

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

  &__user { font-size: 14px; font-weight: 500; color: var(--text1); }

  &__time {
    font-size: 12px;
    color: var(--text4);
    margin-left: auto;
  }

  &__content {
    font-size: 14px;
    line-height: 1.7;
    color: var(--text2);
    margin-bottom: 8px;
    word-break: break-word;
  }

  &__images {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__img {
    width: 150px;
    height: 150px;
    border-radius: var(--radius-sm);
    border: 1px solid var(--line-light);
    cursor: pointer;
  }
}

.review-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* ═══════════════════════════════════════════
   RESPONSIVE
   ═══════════════════════════════════════════ */
@media (max-width: 1199px) {
  .detail-gallery { width: 440px; }
  .gallery-thumbs { width: 60px; }
  .gallery-thumb { width: 56px; height: 56px; }
  .gallery-main { width: 360px; }
  .detail-hero { gap: 32px; padding: 24px; }
}

@media (max-width: 1023px) {
  .detail-hero {
    flex-direction: column;
    gap: 24px;
    padding: 20px;
  }
  .detail-gallery { width: 100%; }
  .gallery-body { flex-direction: column; }
  .gallery-thumbs {
    flex-direction: row;
    width: 100%;
    height: auto;
    max-height: none;
    overflow-x: auto;
    overflow-y: hidden;
    padding-right: 0;
    padding-bottom: 4px;
  }
  .gallery-thumb { width: 56px; height: 56px; flex-shrink: 0; }
  .gallery-main { width: 100%; }
}

@media (max-width: 767px) {
  .detail-hero {
    flex-direction: column;
    gap: 20px;
    padding: 12px;
    border-radius: 0;
    border-left: none;
    border-right: none;
  }
  .detail-gallery { width: 100%; }
  .detail-section {
    padding: 20px 12px;
    border-radius: 0;
    border-left: none;
    border-right: none;
  }
  .detail-sticky-nav { max-width: 100%; margin: 0 -12px; padding: 0 12px; }
  .sticky-nav__tab { padding: 12px 16px; font-size: 14px; }
  .detail-info {
    &__name { font-size: 18px; }
    .price-value { font-size: 24px; }
    &__actions { flex-direction: column; }
  }
  .review-overview { flex-direction: column; gap: 20px; }
}
</style>
