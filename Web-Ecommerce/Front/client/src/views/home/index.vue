<template>
  <div class="home">
    <!-- Full-width Banner -->
    <section class="hero">
      <div class="hero__banner">
        <el-carousel height="360px" :interval="5000" arrow="always">
          <el-carousel-item v-for="(b, i) in banners" :key="i">
            <div class="hero__slide" :style="{ background: b.bg }">
              <div class="hero__slide-text">
                <h2>{{ b.title }}</h2>
                <p>{{ b.subtitle }}</p>
                <el-button type="primary" size="large" round @click="$router.push('/products')">
                  立即选购
                </el-button>
              </div>
              <div class="hero__slide-art">
                <svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="100" cy="100" r="80" fill="rgba(255,255,255,0.15)" />
                  <circle cx="100" cy="100" r="55" fill="rgba(255,255,255,0.2)" />
                  <text x="100" y="95" text-anchor="middle" font-size="48" fill="rgba(255,255,255,0.6)">&#{{ b.iconCode }};</text>
                </svg>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </section>

    <!-- Category Card Floor -->
    <section v-if="categories.length" class="category-floor">
      <div class="category-grid">
        <div
          v-for="(cat, i) in categories"
          :key="cat.id"
          class="category-card"
          :style="{ '--accent': accentColors[i % accentColors.length] }"
          @click="$router.push(`/products?categoryId=${cat.id}`)"
        >
          <span class="category-card__icon">
            <component :is="catIcons[cat.id % catIcons.length]" />
          </span>
          <span class="category-card__name">{{ cat.name }}</span>
          <span class="category-card__hint">
            {{ cat.productCount != null ? `${cat.productCount}件商品` : '去看看' }}
          </span>
        </div>
      </div>
    </section>

    <!-- Trust Features -->
    <section class="features">
      <div class="feature-item">
        <el-icon size="32"><CircleCheck /></el-icon>
        <div>
          <strong>品质保证</strong>
          <p>正品保障 假一赔十</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon size="32"><Van /></el-icon>
        <div>
          <strong>免费配送</strong>
          <p>满99元包邮</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon size="32"><Headset /></el-icon>
        <div>
          <strong>售后无忧</strong>
          <p>7天无理由退换</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon size="32"><Lock /></el-icon>
        <div>
          <strong>安全支付</strong>
          <p>多重加密保障</p>
        </div>
      </div>
    </section>

    <!-- Hot Products -->
    <section class="section">
      <div class="section__header">
        <div class="section__title">
          <span class="section__icon">&#x1F525;</span>
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
          <span class="section__icon">&#x2728;</span>
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

    <!-- Promo Banner -->
    <section class="promo" @click="$router.push('/products')">
      <div class="promo__inner">
        <div>
          <h2>限时特惠</h2>
          <p>精选好物低至5折，数量有限，先到先得</p>
        </div>
        <el-button type="danger" size="large" round>去抢购</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ArrowRight, CircleCheck, Van, Headset, Lock,
  Goods, ShoppingBag, Star, Present,
} from '@element-plus/icons-vue'
import { getHotProducts, getNewProducts, getCategories } from '@/api/product'
import type { Product, Category } from '@shared/types/product'
import ProductCard from '@/components/business/ProductCard.vue'

const catIcons = [Goods, ShoppingBag, Star, Present]

const accentColors = [
  '#667eea', '#f5576c', '#4facfe', '#f093fb',
  '#fa709a', '#30cfd0', '#a18cd1', '#ff6f3f',
  '#43e97b', '#ffc837',
]

const categories = ref<Category[]>([])
const hotProducts = ref<Product[]>([])
const newProducts = ref<Product[]>([])
const hotLoading = ref(false)
const newLoading = ref(false)

const banners = [
  {
    title: '新品首发',
    subtitle: '春季新品限时特惠，全场低至5折',
    bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    iconCode: 'x2728',
  },
  {
    title: '品质数码',
    subtitle: '品牌授权 正品保障 闪电发货',
    bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    iconCode: 'x1F4F1',
  },
  {
    title: '限时秒杀',
    subtitle: '每日10点开抢 超值好货不容错过',
    bg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    iconCode: 'x26A1',
  },
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
}

/* ── Hero Banner ── */
.hero {
  margin-bottom: 32px;

  &__banner {
    border-radius: var(--radius-lg);
    overflow: hidden;
  }

  &__slide {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
    padding: 0 80px;
    color: #fff;
  }

  &__slide-text {
    h2 {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 12px;
    }
    p {
      font-size: 16px;
      opacity: .85;
      margin-bottom: 24px;
    }
  }

  &__slide-art {
    width: 200px;
    flex-shrink: 0;
  }
}

/* ── Category Card Floor ── */
.category-floor {
  margin-bottom: 40px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.category-card {
  --accent: #409eff;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 32px 16px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    background: var(--accent);
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  }

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 14px 36px rgba(0, 0, 0, 0.14);

    .category-card__icon {
      transform: scale(1.12);
    }
  }

  &__icon {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: var(--accent);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    color: #fff;
    transition: transform var(--transition-normal);
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text1);
    margin-top: 2px;
  }

  &__hint {
    font-size: 12px;
    color: var(--text3);
  }
}

/* ── Features ── */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 40px;

  .feature-item {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: var(--shadow-sm);

    .el-icon { color: var(--brand-primary); flex-shrink: 0; }

    strong { font-size: 15px; display: block; margin-bottom: 4px; }
    p     { font-size: 12px; color: var(--text3); margin: 0; }
  }
}

/* ── Section ── */
.section {
  margin-bottom: 40px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 10px;

    h2 {
      font-size: 22px;
      font-weight: 700;
    }
  }

  &__icon {
    font-size: 24px;
  }

  &__more {
    display: flex;
    align-items: center;
    gap: 4px;
    color: var(--text3);
    font-size: 14px;
    transition: color var(--transition-fast);

    &:hover { color: var(--brand-primary); }
  }
}

/* ── Product Grid ── */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* ── Promo ── */
.promo {
  margin-bottom: 40px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;

  &__inner {
    background: linear-gradient(135deg, #ff6f3f 0%, #e6423a 100%);
    padding: 40px 48px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #fff;

    h2 { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
    p  { font-size: 15px; opacity: .85; }
  }
}

/* ── Responsive ── */
@media (max-width: 1440px) {
  .category-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

@media (max-width: 1024px) {
  .hero__slide {
    padding: 0 32px;

    &-text h2 { font-size: 26px; }
    &-art { width: 140px; }
  }

  .category-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
  }

  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .features {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero {
    margin-bottom: 20px;

    &__banner {
      border-radius: 8px;
    }

    &__slide {
      padding: 0 24px;

      &-text h2 { font-size: 22px; }
      &-art { display: none; }
    }
  }

  .category-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .category-card {
    padding: 22px 10px 16px;
    gap: 6px;

    &__icon {
      width: 40px;
      height: 40px;
      font-size: 20px;
    }

    &__name { font-size: 13px; }
    &__hint { font-size: 11px; }
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .features {
    grid-template-columns: 1fr;
  }

  .promo__inner {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
}
</style>
