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
          <span class="detail-info__price">{{ formatPrice(product.price) }}</span>
          <span class="detail-info__original" v-if="product.price < 99999">
            {{ formatPrice(product.price * 1.2) }}
          </span>
        </div>

        <div class="detail-info__meta">
          <div class="meta-item">
            <el-icon><Box /></el-icon>
            <span>库存 {{ product.stock }} 件</span>
          </div>
          <div class="meta-item">
            <el-icon><TrendCharts /></el-icon>
            <span>已售 {{ product.sales }} 件</span>
          </div>
        </div>

        <div class="detail-info__divider" />

        <div class="detail-info__spec" v-if="product.skus?.length">
          <h4>规格</h4>
          <div class="spec-options">
            <span
              v-for="sku in product.skus"
              :key="sku.id"
              :class="['spec-tag', { 'spec-tag--active': selectedSkuId === sku.id }]"
              @click="selectedSkuId = sku.id"
            >
              {{ sku.specValue }}
            </span>
          </div>
        </div>

        <div class="detail-info__qty">
          <h4>数量</h4>
          <el-input-number
            v-model="quantity"
            :min="1"
            :max="product.stock"
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

    <div class="detail-section" v-if="product">
      <h2 class="detail-section__title">商品详情</h2>
      <div class="detail-section__body" v-html="product.description || '<p style=\'text-align:center;color:#999;padding:40px\'>暂无详细描述</p>'" />
    </div>

    <el-empty v-if="!loading && !product" description="商品不存在">
      <el-button type="primary" @click="$router.push('/products')">浏览其他商品</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, TrendCharts, ShoppingCart } from '@element-plus/icons-vue'
import { getProductById } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import type { Product } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref<Product | null>(null)
const loading = ref(false)
const quantity = ref(1)
const addLoading = ref(false)
const activeImg = ref('')
const selectedSkuId = ref(0)

async function loadProduct() {
  loading.value = true
  try {
    const res = await getProductById(Number(route.params.id))
    product.value = res.data
    activeImg.value = product.value.mainImage
  } finally {
    loading.value = false
  }
}

async function handleAddToCart() {
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

onMounted(() => { loadProduct() })
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
      background: linear-gradient(135deg, #ff6f3f, #e6423a);
      border: none;
      color: #fff;

      &:hover {
        background: linear-gradient(135deg, #e85d2f, #d63a32);
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

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  .spec-tag {
    padding: 8px 18px;
    border: 2px solid #eee;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    transition: all .2s;

    &:hover { border-color: #409eff; color: #409eff; }

    &--active {
      border-color: #409eff;
      background: rgba(64,158,255,.08);
      color: #409eff;
      font-weight: 600;
    }
  }
}

/* ── Description ── */
.detail-section {
  background: #fff;
  padding: 32px;
  border-radius: 12px;

  &__title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 2px solid #409eff;
  }

  &__body {
    line-height: 1.8;
    color: #555;
  }
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
