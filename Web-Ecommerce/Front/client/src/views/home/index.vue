<template>
  <div class="home">
    <section class="banner">
      <el-carousel height="360px" :interval="5000">
        <el-carousel-item v-for="item in banners" :key="item.id">
          <img :src="item.imageUrl" :alt="item.title" class="banner__img" />
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="section">
      <div class="section__header">
        <h2>热门商品</h2>
        <router-link to="/products?sort=sales_desc">查看更多</router-link>
      </div>
      <div class="product-grid">
        <product-card v-for="item in hotProducts" :key="item.id" :product="item" />
      </div>
      <el-empty v-if="!hotProducts.length" description="暂无热门商品" />
    </section>

    <section class="section">
      <div class="section__header">
        <h2>新品推荐</h2>
        <router-link to="/products?sort=newest">查看更多</router-link>
      </div>
      <div class="product-grid">
        <product-card v-for="item in newProducts" :key="item.id" :product="item" />
      </div>
      <el-empty v-if="!newProducts.length" description="暂无新品" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHotProducts, getNewProducts } from '@/api/product'
import type { Product } from '@shared/types/product'
import ProductCard from '@/components/business/ProductCard.vue'

const banners = ref([
  { id: 1, title: 'Banner 1', imageUrl: '/images/banner1.jpg' },
])

const hotProducts = ref<Product[]>([])
const newProducts = ref<Product[]>([])

onMounted(async () => {
  try {
    const [hotRes, newRes] = await Promise.all([getHotProducts(), getNewProducts()])
    hotProducts.value = hotRes.data
    newProducts.value = newRes.data
  } catch {
    // handled by interceptor
  }
})
</script>

<style lang="scss" scoped>
.home {
  .banner {
    margin-bottom: 32px;
    border-radius: 8px;
    overflow: hidden;

    &__img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .section {
    margin-bottom: 40px;

    &__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      h2 {
        font-size: 20px;
        font-weight: 600;
      }

      a {
        color: #409eff;
        font-size: 14px;
      }
    }
  }

  .product-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    @media (max-width: 1024px) {
      grid-template-columns: repeat(3, 1fr);
    }

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}
</style>
