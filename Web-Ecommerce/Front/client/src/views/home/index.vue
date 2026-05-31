<template>
  <div class="home">
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
        <el-carousel height="400px" :interval="5000" arrow="always">
          <el-carousel-item v-for="(b, i) in banners" :key="i">
            <div class="hero-carousel__slide" :style="{ background: b.bg }">
              <div class="hero-carousel__text">
                <span class="hero-carousel__tag">{{ b.tag }}</span>
                <h2>{{ b.title }}</h2>
                <p>{{ b.subtitle }}</p>
                <el-button size="large" round class="hero-carousel__btn" @click="$router.push('/products')">
                  立即选购
                </el-button>
              </div>
              <div class="hero-carousel__art" aria-hidden="true">
                <svg viewBox="0 0 160 160" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="80" cy="80" r="75" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="2"/>
                  <circle cx="80" cy="80" r="55" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="1.5" stroke-dasharray="8 4"/>
                  <circle cx="80" cy="80" r="32" fill="rgba(255,255,255,0.12)"/>
                </svg>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- Right: Promo Cards -->
      <div class="hero-promo">
        <div
          v-for="card in rightCards"
          :key="card.title"
          class="hero-promo__card"
          :style="{ background: card.bg }"
          @click="$router.push(card.link)"
        >
          <span class="hero-promo__icon">{{ card.icon }}</span>
          <div class="hero-promo__card-content">
            <span class="hero-promo__card-title">{{ card.title }}</span>
            <span class="hero-promo__card-desc">{{ card.desc }}</span>
          </div>
        </div>
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

    <!-- Bottom Promo -->
    <section class="promo-bottom" @click="$router.push('/products')">
      <div class="promo-bottom__inner">
        <div class="promo-bottom__text">
          <span class="promo-bottom__label">每日精选</span>
          <h2>限时特惠 · 低至5折</h2>
          <p>精选好物限时抢购，数量有限先到先得</p>
        </div>
        <el-button size="large" round class="promo-bottom__btn">去抢购</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  ArrowRight,
  Goods, ShoppingBag, Star, Present, Grid,
} from '@element-plus/icons-vue'
import { getHotProducts, getNewProducts, getCategories } from '@/api/product'
import type { Product, Category } from '@shared/types/product'
import ProductCard from '@/components/business/ProductCard.vue'

const catIcons = [Goods, ShoppingBag, Star, Present]

const categories = ref<Category[]>([])
const hotProducts = ref<Product[]>([])
const newProducts = ref<Product[]>([])
const hotLoading = ref(false)
const newLoading = ref(false)
const activeCatId = ref(0)

const topCategories = computed(() =>
  categories.value.filter(c => !c.parentId).sort((a, b) => a.sortOrder - b.sortOrder)
)

const activePanelCat = computed(() =>
  topCategories.value.find(c => c.id === activeCatId.value) || null
)

const banners = [
  {
    tag: '新品首发',
    title: '春季上新',
    subtitle: '全场低至5折，限时特惠中',
    bg: 'linear-gradient(135deg, #5b7cfa 0%, #7c5cf0 100%)',
  },
  {
    tag: '品质生活',
    title: '精选好物',
    subtitle: '用心甄选 品质保障',
    bg: 'linear-gradient(135deg, #0ea5c0 0%, #06b6b0 100%)',
  },
  {
    tag: '限时秒杀',
    title: '每日10点',
    subtitle: '超值好货不容错过',
    bg: 'linear-gradient(135deg, #f0628b 0%, #e8556e 100%)',
  },
]

const rightCards = [
  {
    icon: '⚡',
    title: '限时秒杀',
    desc: '每日10点开抢',
    bg: 'linear-gradient(160deg, #5b7cfa 0%, #7c5cf0 100%)',
    link: '/products?promo=flash',
  },
  {
    icon: '🎁',
    title: '新人专区',
    desc: '注册即享好礼',
    bg: 'linear-gradient(160deg, #0ea5c0 0%, #06b6b0 100%)',
    link: '/products?promo=new',
  },
  {
    icon: '✨',
    title: '精品推荐',
    desc: '甄选品质好物',
    bg: 'linear-gradient(160deg, #f0628b 0%, #e8556e 100%)',
    link: '/products?sort=rating_desc',
  },
  {
    icon: '🎫',
    title: '领券中心',
    desc: '大额优惠券',
    bg: 'linear-gradient(160deg, #f49b3f 0%, #f0628b 100%)',
    link: '/coupons',
  },
]

const featureList = [
  { icon: '🛡', label: '品质保证', desc: '正品保障 假一赔十' },
  { icon: '🚚', label: '免费配送', desc: '满99元包邮' },
  { icon: '💬', label: '售后无忧', desc: '7天无理由退换' },
  { icon: '🔒', label: '安全支付', desc: '多重加密保障' },
]

onMounted(async () => {
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
})
</script>

<style lang="scss" scoped>
.home {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 0 48px;
}

/* ══════════════════════════════════════════
   Three-column Hero
   ══════════════════════════════════════════ */
.hero-three-col {
  display: flex;
  gap: 14px;
  margin: 20px 0 36px;
  height: 400px;
}

/* ── Left: Category Sidebar ── */
$cat-accent: #5b7cfa;
$cat-bg: #f8f9ff;
$cat-hover-bg: #eef1ff;
$cat-text: #2c3a5e;
$cat-subtle: #7b8bb4;

.hero-cat {
  width: 234px;
  flex-shrink: 0;
  height: 100%;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .05), 0 4px 14px rgba(0, 0, 0, .04);
  position: relative;
  overflow: visible;
  display: flex;
  flex-direction: column;

  &__header {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #5b7cfa 0%, #4c6aee 100%);
    padding: 13px 18px;
    border-radius: 10px 10px 0 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__header-icon {
    font-size: 16px;
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 8px 0;
    flex: 1;
    overflow-y: auto;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 18px;
    cursor: pointer;
    color: $cat-text;
    font-size: 13px;
    transition: background .18s, color .18s, padding-left .18s;

    &:hover,
    &.is-active {
      background: $cat-hover-bg;
      color: $cat-accent;
      font-weight: 600;
      padding-left: 22px;

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

  &__name {
    flex: 1;
  }

  &__arrow {
    font-size: 11px;
    opacity: 0;
    transition: opacity .18s, color .18s;
    color: $cat-subtle;
  }

  // Hover popup panel
  &__panel {
    position: absolute;
    left: calc(100% + 2px);
    top: 0;
    width: 552px;
    min-height: 400px;
    background: #fff;
    border-radius: 0 10px 10px 10px;
    box-shadow: 6px 12px 40px rgba(30, 40, 80, .1);
    z-index: 100;
    padding: 22px 26px;
    display: flex;
    flex-wrap: wrap;
    align-content: flex-start;
    gap: 18px;
  }

  &__sub-group {
    width: calc(50% - 9px);
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
    border-bottom: 1px solid #eef1f8;
    width: 100%;

    &:hover {
      color: $cat-accent;
    }

    .el-icon {
      font-size: 11px;
    }
  }

  &__sub-links {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__sub-link {
    font-size: 12px;
    color: #6b7a9e;
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
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .04), 0 4px 12px rgba(0, 0, 0, .03);

  :deep(.el-carousel) {
    height: 100% !important;
  }

  :deep(.el-carousel__container) {
    height: 100% !important;
  }

  &__slide {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
    padding: 0 64px;
    color: #fff;
  }

  &__text {
    position: relative;
    z-index: 1;

    h2 {
      font-size: 34px;
      font-weight: 700;
      margin-bottom: 8px;
      letter-spacing: 1px;
    }
    p {
      font-size: 15px;
      opacity: .85;
      margin-bottom: 24px;
      font-weight: 400;
    }
  }

  &__tag {
    display: inline-block;
    font-size: 11px;
    font-weight: 500;
    padding: 4px 14px;
    border-radius: 20px;
    background: rgba(255, 255, 255, .22);
    backdrop-filter: blur(4px);
    margin-bottom: 12px;
    letter-spacing: .5px;
  }

  &__btn {
    background: rgba(255, 255, 255, .95) !important;
    color: #4c6aee !important;
    border: none !important;
    font-weight: 600 !important;
    padding: 10px 28px !important;
    font-size: 14px !important;
    transition: all .2s;

    &:hover {
      background: #fff !important;
      box-shadow: 0 4px 16px rgba(0, 0, 0, .1);
      transform: translateY(-1px);
    }
  }

  &__art {
    width: 160px;
    flex-shrink: 0;
    opacity: .7;
  }
}

/* ── Right: Promo Cards ── */
.hero-promo {
  width: 240px;
  flex-shrink: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__card {
    flex: 1;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    transition: transform .25s, box-shadow .25s;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    position: relative;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 24px rgba(80, 50, 30, .1);
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(0,0,0,.08) 100%);
      pointer-events: none;
    }
  }

  &__icon {
    font-size: 26px;
    position: relative;
    z-index: 1;
  }

  &__card-content {
    text-align: center;
    color: #fff;
    position: relative;
    z-index: 1;
  }

  &__card-title {
    display: block;
    font-size: 16px;
    font-weight: 700;
    margin-bottom: 3px;
    letter-spacing: .5px;
  }

  &__card-desc {
    font-size: 12px;
    opacity: .82;
  }
}

/* ── Features ── */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 44px;

  .feature-item {
    background: #fff;
    border-radius: 10px;
    padding: 22px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, .04);
    transition: transform .2s, box-shadow .2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 14px rgba(0, 0, 0, .06);
    }

    &__icon {
      font-size: 28px;
      flex-shrink: 0;
    }

    strong { font-size: 14px; display: block; margin-bottom: 3px; color: $cat-text; }
    p     { font-size: 12px; color: #9b9fb0; margin: 0; }
  }
}

/* ── Section ── */
.section {
  margin-bottom: 44px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 22px;
  }

  &__title {
    h2 {
      font-size: 20px;
      font-weight: 700;
      color: $cat-text;
      position: relative;
      padding-left: 14px;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 3px;
        bottom: 3px;
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
    color: #9b9fb0;
    font-size: 13px;
    transition: color .2s;

    &:hover { color: $cat-accent; }
  }
}

/* ── Product Grid ── */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* ── Bottom Promo ── */
.promo-bottom {
  margin-bottom: 0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 28px rgba(80, 50, 30, .06);
  }

  &__inner {
    background: linear-gradient(135deg, #5b7cfa 0%, #7c5cf0 100%);
    padding: 42px 52px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #fff;
  }

  &__label {
    display: inline-block;
    font-size: 12px;
    padding: 3px 12px;
    border-radius: 20px;
    background: rgba(255, 255, 255, .22);
    margin-bottom: 10px;
  }

  &__text {
    h2 { font-size: 26px; font-weight: 700; margin-bottom: 6px; }
    p  { font-size: 14px; opacity: .85; margin: 0; }
  }

  &__btn {
    background: rgba(255, 255, 255, .95) !important;
    color: #4c6aee !important;
    border: none !important;
    font-weight: 600 !important;
    font-size: 14px !important;
    padding: 12px 32px !important;
    transition: all .2s;

    &:hover {
      background: #fff !important;
      box-shadow: 0 4px 16px rgba(0, 0, 0, .08);
    }
  }
}

/* ══════════════════════════════════════════
   Responsive
   ══════════════════════════════════════════ */
@media (max-width: 1280px) {
  .hero-cat {
    width: 200px;
  }
  .hero-promo {
    width: 200px;
  }
  .hero-cat__panel {
    width: 480px;
  }
}

@media (max-width: 1024px) {
  .hero-cat {
    width: 180px;

    &__panel {
      display: none;
    }
  }
  .hero-promo {
    display: none;
  }
  .hero-carousel__slide {
    padding: 0 36px;
  }
  .hero-carousel__text h2 {
    font-size: 26px;
  }
  .hero-carousel__art {
    width: 120px;
  }
  .product-grid {
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

    &__slide {
      padding: 0 24px;
    }

    &__text h2 {
      font-size: 20px;
    }

    &__tag {
      font-size: 10px;
    }

    &__art {
      display: none;
    }
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .features {
    grid-template-columns: 1fr;
  }

  .promo-bottom__inner {
    flex-direction: column;
    text-align: center;
    gap: 18px;
    padding: 32px 24px;
  }
}
</style>
