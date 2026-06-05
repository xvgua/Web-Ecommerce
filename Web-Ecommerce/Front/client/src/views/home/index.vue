<template>
  <div class="home">
    <!-- Announcement Bar -->
    <AnnouncementBar />

    <!-- Three-column Hero: Categories | Carousel | Promo Cards -->
    <section class="hero-three-col">
      <!-- Left: Category Sidebar -->
      <div class="hero-cat" @mouseleave="activeCatId = 0">
        <div class="hero-cat__header">
          <el-icon class="hero-cat__header-icon"><Grid /></el-icon>
          <span>全部商品分类</span>
        </div>
        <ul class="hero-cat__list">
          <li
            v-for="(cat, i) in topCategories"
            :key="cat.id"
            class="hero-cat__item"
            :class="{ 'is-active': activeCatId === cat.id }"
            :style="{ animationDelay: `${i * 30}ms` }"
            @mouseenter="activeCatId = cat.id"
            @click="$router.push(`/products?categoryId=${cat.id}`)"
          >
            <span class="hero-cat__icon">
              <component :is="catIcons[cat.id % catIcons.length]" :size="15" />
            </span>
            <span class="hero-cat__name">{{ cat.name }}</span>
            <el-icon class="hero-cat__arrow"><ArrowRight /></el-icon>
          </li>
        </ul>
        <!-- Hover secondary panel -->
        <div
          v-show="activeCatId && activePanelCat?.children?.length"
          class="hero-cat__panel"
          @mouseenter="activeCatId = activePanelCat!.id"
        >
          <div
            v-for="child in activePanelCat?.children"
            :key="child.id"
            class="hero-cat__sub-group"
          >
            <router-link
              :to="`/products?categoryId=${child.id}`"
              class="hero-cat__sub-title"
            >
              {{ child.name }}
              <el-icon><ArrowRight /></el-icon>
            </router-link>
            <div class="hero-cat__sub-links" v-if="child.children?.length">
              <router-link
                v-for="sub in child.children"
                :key="sub.id"
                :to="`/products?categoryId=${sub.id}`"
                class="hero-cat__sub-link"
              >
                {{ sub.name }}
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <!-- Center: Carousel -->
      <div class="hero-carousel">
        <template v-if="banners.length > 1">
          <el-carousel height="400px" :interval="5000" arrow="always">
            <el-carousel-item v-for="b in banners" :key="b.id">
              <a v-if="b.linkUrl" :href="b.linkUrl" class="hero-carousel__link">
                <el-image :src="b.imageUrl" :alt="b.title" fit="cover" class="hero-carousel__img">
                  <template #error><div class="hero-carousel__placeholder">加载失败</div></template>
                </el-image>
              </a>
              <el-image v-else :src="b.imageUrl" :alt="b.title" fit="cover" class="hero-carousel__img">
                <template #error><div class="hero-carousel__placeholder">加载失败</div></template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
        </template>
        <template v-else-if="banners.length === 1">
          <el-carousel height="400px" :interval="5000" arrow="never" indicator-position="none">
            <el-carousel-item>
              <a v-if="banners[0].linkUrl" :href="banners[0].linkUrl" class="hero-carousel__link">
                <el-image :src="banners[0].imageUrl" :alt="banners[0].title" fit="cover" class="hero-carousel__img">
                  <template #error><div class="hero-carousel__placeholder">加载失败</div></template>
                </el-image>
              </a>
              <el-image v-else :src="banners[0].imageUrl" :alt="banners[0].title" fit="cover" class="hero-carousel__img">
                <template #error><div class="hero-carousel__placeholder">加载失败</div></template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
        </template>
        <div v-else class="hero-carousel__placeholder hero-carousel__placeholder--full">
          <span>暂无轮播</span>
        </div>
      </div>

      <!-- Right: User Sidebar -->
      <div class="hero-right">
        <UserSidebar />
      </div>
    </section>

    <!-- Trust Features -->
    <section class="features">
      <div class="feature-item" v-for="f in featureList" :key="f.label">
        <span class="feature-item__icon">{{ f.icon }}</span>
        <div>
          <strong>{{ f.label }}</strong>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Hot Products -->
    <section class="section">
      <div class="section__header">
        <div class="section__title">
          <h2>热门商品</h2>
        </div>
        <router-link to="/products?sort=sales_desc" class="section__more">
          查看更多 <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
      <div class="product-grid" v-loading="hotLoading">
        <product-card v-for="item in hotProducts" :key="item.id" :product="item" />
      </div>
      <el-empty v-if="!hotLoading && !hotProducts.length" description="暂无热门商品" />
    </section>

    <!-- New Arrivals -->
    <section class="section">
      <div class="section__header">
        <div class="section__title">
          <h2>新品推荐</h2>
        </div>
        <router-link to="/products?sort=newest" class="section__more">
          查看更多 <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
      <div class="product-grid" v-loading="newLoading">
        <product-card v-for="item in newProducts" :key="item.id" :product="item" />
      </div>
      <el-empty v-if="!newLoading && !newProducts.length" description="暂无新品" />
    </section>

    <!-- Seckill Section -->
    <section class="home-seckill" v-if="currentSeckill">
      <div class="home-seckill__header">
        <div class="home-seckill__header-info">
          <h2 class="home-seckill__title">
            <el-icon :size="22"><Timer /></el-icon>
            {{ currentSeckill.name }} 限时促销
          </h2>
          <div class="home-seckill__countdown">
            <span class="countdown-label">{{ countdownLabel }}</span>
            <span class="countdown-timer">{{ countdownText }}</span>
          </div>
        </div>
        <el-button round class="home-seckill__more" @click="$router.push('/seckill')">
          查看更多秒杀 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="home-seckill__products">
        <div
          v-for="sp in currentSeckill.products?.slice(0, 4)"
          :key="sp.id"
          class="seckill-product-card"
        >
          <div class="seckill-product-card__image" @click="goProduct(sp.productId)">
            <ProductImage :src="sp.productImage" :seed="sp.productName + sp.productId" fit="cover" />
            <div class="seckill-tag" v-if="sp.remainStock === 0">已售罄</div>
          </div>
          <div class="seckill-product-card__info">
            <h4 class="product-name" @click="goProduct(sp.productId)">{{ sp.productName }}</h4>
            <p class="spec-desc" v-if="sp.specDesc">{{ sp.specDesc }}</p>
            <div class="price-row">
              <span class="seckill-price">&yen;{{ sp.seckillPrice }}</span>
              <span class="original-price" v-if="sp.originalPrice">&yen;{{ sp.originalPrice }}</span>
            </div>
            <div class="stock-bar">
              <div class="stock-bar__inner" :style="{ width: stockPercent(sp) + '%' }"></div>
            </div>
            <div class="stock-info">
              <span>已抢{{ sp.seckillStock - sp.remainStock }}件</span>
              <span>剩余{{ sp.remainStock }}件</span>
            </div>
            <el-button
              type="danger"
              :disabled="sp.remainStock === 0 || hasPurchased(sp)"
              @click="goSeckill(sp)"
              class="seckill-btn"
              :loading="seckillingId === sp.id"
            >
              {{ sp.remainStock === 0 ? '已售罄' : hasPurchased(sp) ? '已抢购' : '立即秒杀' }}
            </el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Goods, ShoppingBag, Star, Present, Grid, Timer,
} from '@element-plus/icons-vue'
import { getHotProducts, getNewProducts, getCategories } from '@/api/product'
import { getActiveActivities, getMyPurchasedSeckill } from '@/api/seckill'
import { getBanners } from '@/api/banner'
import type { Product, Category } from '@shared/types/product'
import type { SeckillActivity, SeckillProduct } from '@shared/types/seckill'
import type { Banner } from '@shared/types'
import ProductCard from '@/components/business/ProductCard.vue'
import ProductImage from '@/components/common/ProductImage.vue'
import AnnouncementBar from '@/components/business/AnnouncementBar.vue'
import UserSidebar from '@/components/business/UserSidebar.vue'

const catIcons = [Goods, ShoppingBag, Star, Present]

const categories = ref<Category[]>([])
const hotProducts = ref<Product[]>([])
const newProducts = ref<Product[]>([])
const hotLoading = ref(false)
const newLoading = ref(false)
const activeCatId = ref(0)
const router = useRouter()
const currentSeckill = ref<SeckillActivity | null>(null)
const now = ref(Date.now())
const purchasedIds = ref<Set<number>>(new Set())
const seckillingId = ref<number | null>(null)
let seckillTimer: number | null = null

const hasPurchased = (sp: SeckillProduct) => purchasedIds.value.has(sp.id)

const countdownLabel = computed(() => {
  if (!currentSeckill.value) return ''
  const start = new Date(currentSeckill.value.startTime).getTime()
  const end = new Date(currentSeckill.value.endTime).getTime()
  if (now.value < start) return '距离开始'
  if (now.value < end) return '距离结束'
  return '已结束'
})

const countdownText = computed(() => {
  if (!currentSeckill.value) return '--:--:--:----'
  const start = new Date(currentSeckill.value.startTime).getTime()
  const end = new Date(currentSeckill.value.endTime).getTime()
  let diff: number
  if (now.value < start) diff = start - now.value
  else if (now.value < end) diff = end - now.value
  else return '00:00:00:0000'

  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  const ms = diff % 1000
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}:${String(ms).padStart(4, '0')}`
})

function stockPercent(sp: SeckillProduct) {
  if (sp.seckillStock === 0) return 0
  return Math.round((sp.remainStock / sp.seckillStock) * 100)
}

function goProduct(productId: number) {
  router.push(`/products/${productId}`)
}

function goSeckill(sp: SeckillProduct) {
  seckillingId.value = sp.id
  router.push(`/order/confirm?seckillProductId=${sp.id}`)
  seckillingId.value = null
}

const topCategories = computed(() =>
  categories.value.filter(c => !c.parentId).sort((a, b) => a.sortOrder - b.sortOrder)
)

const activePanelCat = computed(() =>
  topCategories.value.find(c => c.id === activeCatId.value) || null
)

const banners = ref<Banner[]>([])

const featureList = [
  { icon: '🛡', label: '品质保证', desc: '正品保障 假一赔十' },
  { icon: '🚚', label: '免费配送', desc: '满99元包邮' },
  { icon: '💬', label: '售后无忧', desc: '7天无理由退换' },
  { icon: '🔒', label: '安全支付', desc: '多重加密保障' },
]

onMounted(async () => {
  try {
    const res = await getBanners()
    banners.value = res.data
  } catch { /* handled by interceptor */ }

  try {
    const cats = await getCategories()
    categories.value = cats.data
  } catch { /* handled by interceptor */ }

  hotLoading.value = true
  newLoading.value = true
  try {
    const [hotRes, newRes] = await Promise.all([
      getHotProducts(),
      getNewProducts(),
    ])
    hotProducts.value = hotRes.data
    newProducts.value = newRes.data
  } catch { /* handled by interceptor */ }
  finally {
    hotLoading.value = false
    newLoading.value = false
  }

  try {
    const [seckillRes, purchasedRes] = await Promise.all([
      getActiveActivities(),
      getMyPurchasedSeckill().catch(() => ({ data: [] as number[] })),
    ])
    purchasedIds.value = new Set(purchasedRes.data || [])
    if (seckillRes.data?.length) {
      currentSeckill.value = seckillRes.data[0]
    }
  } catch { /* handled by interceptor */ }

  seckillTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 50)
})

onUnmounted(() => {
  if (seckillTimer) clearInterval(seckillTimer)
})
</script>

<style lang="scss" scoped>
$cat-accent: #4EAB8E;
$cat-accent-hover: #5FC0A2;
$cat-bg: #F0F8F5;
$cat-hover-bg: #E6F3EE;
$cat-text: #1A2C26;
$cat-subtle: #6D9A8E;

.home {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 0 64px;
}

/* ══════════════════════════════════════════
   Three-column Hero
   ══════════════════════════════════════════ */
.hero-three-col {
  display: flex;
  gap: 16px;
  margin: 20px 0 48px;
  height: 400px;
}

/* ── Left: Category Sidebar ── */
.hero-cat {
  width: 240px;
  flex-shrink: 0;
  height: 100%;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: visible;
  display: flex;
  flex-direction: column;

  &__header {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #3D8F76 0%, #4EAB8E 100%);
    padding: 14px 20px;
    border-radius: 12px 12px 0 0;
    display: flex;
    align-items: center;
    gap: 8px;
    letter-spacing: 0.5px;
  }

  &__header-icon {
    font-size: 17px;
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 6px 0;
    flex: 1;
    overflow-y: auto;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 11px 20px;
    cursor: pointer;
    color: $cat-text;
    font-size: 13px;
    transition: background .2s, color .2s, padding-left .2s;

    &:hover,
    &.is-active {
      background: $cat-hover-bg;
      color: $cat-accent;
      font-weight: 600;
      padding-left: 26px;

      .hero-cat__arrow {
        opacity: 1;
        color: $cat-accent;
      }
    }
  }

  &__icon {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: $cat-subtle;
  }

  &__name { flex: 1; }

  &__arrow {
    font-size: 11px;
    opacity: 0;
    transition: opacity .2s, color .2s;
    color: $cat-subtle;
  }

  &__panel {
    position: absolute;
    left: calc(100% + 4px);
    top: 0;
    width: 556px;
    min-height: 400px;
    background: #fff;
    border-radius: 0 12px 12px 12px;
    box-shadow: 6px 12px 40px rgba(40, 95, 75, 0.10);
    z-index: 100;
    padding: 24px 28px;
    display: flex;
    flex-wrap: wrap;
    align-content: flex-start;
    gap: 20px;
  }

  &__sub-group {
    width: calc(50% - 10px);
  }

  &__sub-title {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 14px;
    font-weight: 600;
    color: $cat-text;
    margin-bottom: 10px;
    transition: color .15s;
    padding-bottom: 6px;
    border-bottom: 1px solid $cat-bg;
    width: 100%;

    &:hover { color: $cat-accent; }

    .el-icon { font-size: 11px; }
  }

  &__sub-links {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__sub-link {
    font-size: 12px;
    color: $cat-subtle;
    padding: 4px 12px;
    background: $cat-bg;
    border-radius: 14px;
    transition: all .18s;

    &:hover {
      color: $cat-accent;
      background: $cat-hover-bg;
    }
  }
}

/* ── Center: Carousel ── */
.hero-carousel {
  flex: 1;
  min-width: 0;
  height: 100%;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);

  :deep(.el-carousel),
  :deep(.el-carousel__container) {
    height: 100% !important;
  }

  &__link {
    display: block;
    width: 100%;
    height: 100%;
  }

  &__img {
    width: 100%;
    height: 100%;

    :deep(img) { object-fit: cover; }
  }

  &__placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: var(--bg3);
    color: var(--text3);
    font-size: 14px;

    &--full { height: 400px; }
  }
}

/* ── Right: User Sidebar ── */
.hero-right {
  width: 290px;
  flex-shrink: 0;
  height: 100%;

  :deep(.user-sidebar) {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
}

/* ── Features ── */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 56px;

  .feature-item {
    background: var(--bg1);
    border-radius: var(--radius-lg);
    padding: 24px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    transition: transform .2s, box-shadow .2s;
    border: 1px solid var(--line-light);

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-lg);
    }

    &__icon { font-size: 28px; flex-shrink: 0; }
    strong { font-size: 14px; display: block; margin-bottom: 4px; color: $cat-text; font-weight: 600; }
    p     { font-size: 12px; color: $cat-subtle; margin: 0; }
  }
}

/* ── Section ── */
.section {
  margin-bottom: 56px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 28px;
  }

  &__title {
    h2 {
      font-size: 22px;
      font-weight: 700;
      color: $cat-text;
      position: relative;
      padding-left: 16px;
      letter-spacing: -0.3px;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 4px;
        bottom: 4px;
        width: 3px;
        border-radius: 2px;
        background: $cat-accent;
      }
    }
  }

  &__more {
    display: flex;
    align-items: center;
    gap: 4px;
    color: $cat-subtle;
    font-size: 13px;
    font-weight: 500;
    transition: color .2s;

    &:hover { color: $cat-accent; }
  }
}

/* ── Product Grid ── */
.product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 18px;
}

/* ── Home Seckill Section ── */
.home-seckill {
  margin-bottom: 56px;

  &__header {
    background: linear-gradient(135deg, #3D8F76 0%, #4EAB8E 100%);
    border-radius: var(--radius-lg);
    padding: 22px 28px;
    margin-bottom: 18px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #fff;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__header-info {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
  }

  &__title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__countdown {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__more {
    background: rgba(255, 255, 255, 0.95) !important;
    color: $cat-accent !important;
    border: none !important;
    font-weight: 600 !important;
    border-radius: var(--radius-full) !important;

    &:hover {
      background: #fff !important;
      transform: scale(1.02);
    }
  }

  &__products {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 18px;
  }
}

.countdown-label {
  font-size: 13px;
  opacity: 0.85;
}

.countdown-timer {
  font-size: 26px;
  font-weight: 700;
  font-family: 'Outfit', 'Courier New', monospace;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
}

/* Seckill Product Card (reused from seckill page) */
.seckill-product-card {
  background: var(--bg1);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--line-light);
  transition: box-shadow .3s, transform .3s;

  &:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
  }

  &__image {
    position: relative;
    width: 100%;
    height: 200px;
    cursor: pointer;
    overflow: hidden;
    background: var(--bg3);

    :deep(.product-image),
    :deep(.el-image),
    :deep(img) {
      width: 100%;
      height: 100%;
    }
  }

  &__info { padding: 14px; }
}

.seckill-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.product-name {
  margin: 0 0 6px;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;

  &:hover { color: $cat-accent; }
}

.spec-desc {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text3);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.seckill-price {
  color: $cat-accent;
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.original-price {
  color: var(--text4);
  font-size: 13px;
  text-decoration: line-through;
}

.stock-bar {
  height: 5px;
  background: $cat-hover-bg;
  border-radius: 3px;
  margin-bottom: 6px;
  overflow: hidden;

  &__inner {
    height: 100%;
    background: linear-gradient(90deg, $cat-accent 0%, $cat-accent-hover 100%);
    border-radius: 3px;
    transition: width .3s;
  }
}

.stock-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text3);
  margin-bottom: 10px;
}

.seckill-btn { width: 100%; }

/* ══════════════════════════════════════════
   Responsive
   ══════════════════════════════════════════ */
@media (max-width: 1280px) {
  .hero-cat { width: 210px; }
  .hero-right { width: 250px; }
  .hero-cat__panel { width: 480px; }
  .product-grid { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 1024px) {
  .hero-cat {
    width: 180px;

    &__panel {
      display: none;
    }
  }
  .hero-right {
    display: none;
  }

  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .home-seckill__products {
    grid-template-columns: repeat(3, 1fr);
  }
  .features {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-three-col {
    flex-direction: column;
    gap: 10px;
    height: auto;
  }

  .hero-cat {
    width: 100%;
    height: auto;

    &__header {
      border-radius: 10px;
    }

    &__list {
      display: flex;
      flex-wrap: wrap;
      padding: 8px;
      overflow-y: visible;
    }

    &__item {
      width: 25%;
      flex-direction: column;
      gap: 4px;
      padding: 10px 4px;
      font-size: 11px;
      text-align: center;
      border-radius: 8px;

      &:hover,
      &.is-active {
        padding-left: 4px;
      }

      .hero-cat__arrow {
        display: none;
      }
    }

    &__panel {
      display: none;
    }
  }

  .hero-carousel {
    height: auto;
    border-radius: 8px;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .home-seckill__products {
    grid-template-columns: repeat(2, 1fr);
  }

  .home-seckill__header {
    flex-direction: column;
    text-align: center;
  }

  .home-seckill__header-info {
    flex-direction: column;
    gap: 12px;
  }

  .features {
    grid-template-columns: 1fr;
  }
}
</style>
