<template>
  <div class="favorites-page">
    <h1 class="page-title">我的收藏</h1>

    <div class="favorites-list" v-if="products.length > 0" v-loading="loading">
      <div
        v-for="product in products"
        :key="product.id"
        class="favorite-item"
      >
        <div class="favorite-item__image" @click="goDetail(product.id)">
          <ProductImage
            :src="product.mainImage"
            :seed="product.name + product.id"
            fit="cover"
          />
        </div>
        <div class="favorite-item__info" @click="goDetail(product.id)">
          <h3 class="favorite-item__name">{{ product.name }}</h3>
          <div class="favorite-item__meta">
            <span class="favorite-item__price">{{ formatPrice(product.price) }}</span>
            <span class="favorite-item__stock" v-if="product.status === 1">库存 {{ product.stock }} 件</span>
            <el-tag type="info" size="small" v-else>已下架</el-tag>
          </div>
        </div>
        <div class="favorite-item__actions">
          <el-button
            type="primary"
            size="default"
            :disabled="product.status !== 1"
            :loading="addLoadingMap[product.id]"
            @click.stop="handleAddToCart(product)"
          >
            <el-icon><ShoppingCart /></el-icon>
            {{ product.status === 1 ? '加入购物车' : '已下架' }}
          </el-button>
          <el-button
            type="default"
            size="default"
            :loading="removeLoadingMap[product.id]"
            @click.stop="handleRemove(product)"
          >
            取消收藏
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="暂无收藏，去逛逛吧">
      <el-button type="primary" @click="$router.push('/products')">浏览商品</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import type { Product } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const cartStore = useCartStore()

const products = ref<Product[]>([])
const loading = ref(false)
const addLoadingMap = reactive<Record<number, boolean>>({})
const removeLoadingMap = reactive<Record<number, boolean>>({})

async function loadFavorites() {
  loading.value = true
  try {
    const res = await getFavoriteList()
    products.value = res.data
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/products/${id}`)
}

async function handleAddToCart(product: Product) {
  addLoadingMap[product.id] = true
  try {
    await cartStore.addItem(product.id, 0, 1)
    ElMessage.success('已加入购物车')
  } finally {
    addLoadingMap[product.id] = false
  }
}

async function handleRemove(product: Product) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${product.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  removeLoadingMap[product.id] = true
  try {
    await removeFavorite(product.id)
    ElMessage.success('已取消收藏')
    products.value = products.value.filter((p) => p.id !== product.id)
  } finally {
    removeLoadingMap[product.id] = false
  }
}

onMounted(() => {
  loadFavorites()
})
</script>

<style lang="scss" scoped>
.favorites-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
}

.favorite-item {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 12px;
  transition: box-shadow .2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, .06);
  }

  &__image {
    width: 100px;
    height: 100px;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
    cursor: pointer;
  }

  &__name {
    font-size: 16px;
    font-weight: 500;
    color: #1a1a1a;
    margin-bottom: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__price {
    font-size: 18px;
    font-weight: 700;
    color: #e6423a;
  }

  &__stock {
    font-size: 13px;
    color: #999;
  }

  &__actions {
    display: flex;
    gap: 10px;
    flex-shrink: 0;
  }
}

@media (max-width: 768px) {
  .favorite-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;

    &__image {
      width: 100%;
      height: 200px;
    }

    &__actions {
      width: 100%;
      justify-content: flex-end;
    }
  }
}
</style>
