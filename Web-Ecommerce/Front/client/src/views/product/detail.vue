<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-main" v-if="product">
      <div class="detail-image">
        <el-image :src="product.mainImage" fit="cover" :preview-src-list="product.images" />
      </div>
      <div class="detail-info">
        <h1 class="detail-name">{{ product.name }}</h1>
        <div class="detail-price">{{ formatPrice(product.price) }}</div>
        <div class="detail-meta">
          <span>库存: {{ product.stock }}</span>
          <span>销量: {{ product.sales }}</span>
          <span>分类: {{ product.categoryName }}</span>
        </div>
        <div class="detail-actions">
          <el-input-number v-model="quantity" :min="1" :max="product.stock" size="large" />
          <el-button type="danger" size="large" :loading="addLoading" @click="handleAddToCart">加入购物车</el-button>
          <el-button type="primary" size="large" @click="handleBuyNow">立即购买</el-button>
        </div>
      </div>
    </div>

    <div class="detail-description" v-if="product">
      <h2>商品详情</h2>
      <div v-html="product.description" class="detail-description__content" />
    </div>

    <el-empty v-if="!loading && !product" description="商品不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductById } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import type { Product } from '@shared/types/product'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref<Product | null>(null)
const loading = ref(false)
const quantity = ref(1)
const addLoading = ref(false)

async function loadProduct() {
  loading.value = true
  try {
    const res = await getProductById(Number(route.params.id))
    product.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleAddToCart() {
  addLoading.value = true
  try {
    await cartStore.addItem(product.value!.id, 0, quantity.value)
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
})
</script>

<style lang="scss" scoped>
.product-detail {
  .detail-main {
    display: flex;
    gap: 40px;
    background: #fff;
    padding: 32px;
    border-radius: 8px;
    margin-bottom: 24px;
  }

  .detail-image {
    width: 420px;
    flex-shrink: 0;

    .el-image {
      width: 100%;
      aspect-ratio: 1;
      border-radius: 8px;
    }
  }

  .detail-info {
    flex: 1;
  }

  .detail-name {
    font-size: 22px;
    font-weight: 600;
    margin-bottom: 16px;
  }

  .detail-price {
    font-size: 28px;
    font-weight: 700;
    color: #e6423a;
    margin-bottom: 16px;
  }

  .detail-meta {
    display: flex;
    gap: 24px;
    color: #999;
    font-size: 14px;
    margin-bottom: 24px;
  }

  .detail-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .detail-description {
    background: #fff;
    padding: 32px;
    border-radius: 8px;

    h2 {
      font-size: 18px;
      margin-bottom: 16px;
    }

    &__content {
      line-height: 1.8;
    }
  }
}
</style>
