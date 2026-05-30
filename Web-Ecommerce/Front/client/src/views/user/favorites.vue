<template>
  <div class="favorites-page">
    <h1 class="page-title">我的收藏</h1>

    <div class="favorites-content" v-if="favorites.length">
      <div class="favorites-items" v-loading="loading">
        <div v-for="fav in favorites" :key="fav.id" class="favorite-item">
          <div class="favorite-item__img" @click="goDetail(fav.productId)">
            <ProductImage :src="fav.productImage" :seed="fav.productName + fav.productId" fit="cover" />
          </div>
          <div class="favorite-item__info">
            <div class="favorite-item__name" @click="goDetail(fav.productId)">{{ fav.productName }}</div>
            <div class="favorite-item__spec">
              <template v-if="fav.skuId && fav.specDesc">
                <span>{{ fav.specDesc }}</span>
              </template>
              <el-popover
                :visible="specPopoverId === fav.id"
                placement="bottom"
                :width="220"
                trigger="click"
                @show="loadProductSkus(fav.productId)"
                @hide="specPopoverId = 0"
              >
                <template #reference>
                  <el-button text size="small" type="primary" @click="specPopoverId = specPopoverId === fav.id ? 0 : fav.id">
                    {{ fav.skuId ? '换规格' : '请选择规格' }}
                  </el-button>
                </template>
                <div class="spec-popover-list" v-if="productSkuMap[fav.productId]?.length">
                  <div
                    v-for="sku in productSkuMap[fav.productId]"
                    :key="sku.id"
                    :class="['spec-option', {
                      'spec-option--active': sku.id === fav.skuId,
                      'spec-option--disabled': sku.stock === 0
                    }]"
                    @click="sku.stock > 0 && handleSwitchSku(fav, sku)"
                  >
                    <img v-if="sku.image" :src="sku.image" class="spec-option__img" />
                    <div class="spec-option__info">
                      <span class="spec-option__name">{{ sku.specValue }}</span>
                      <span class="spec-option__label">{{ sku.specName }}</span>
                    </div>
                    <span class="spec-option__price">{{ formatPrice(sku.price) }}</span>
                  </div>
                </div>
                <el-empty v-else description="暂无其他规格" :image-size="40" />
              </el-popover>
            </div>
          </div>
          <div class="favorite-item__price">{{ formatPrice(fav.price) }}</div>
          <div class="favorite-item__actions">
            <el-button
              type="primary"
              size="small"
              :disabled="fav.stock === 0"
              :loading="addLoadingMap[fav.productId]"
              @click.stop="handleAddToCart(fav)"
            >
              加入购物车
            </el-button>
            <el-button
              size="small"
              :loading="removeLoadingMap[fav.productId]"
              @click.stop="handleRemove(fav)"
            >
              取消收藏
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else v-loading="loading" description="暂无收藏，去逛逛吧">
      <el-button type="primary" @click="$router.push('/products')">浏览商品</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFavoriteList, removeFavorite, updateFavoriteSku } from '@/api/favorite'
import type { Favorite } from '@/api/favorite'
import { getProductById } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import type { ProductSku } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const cartStore = useCartStore()

const favorites = ref<Favorite[]>([])
const loading = ref(false)
const addLoadingMap = reactive<Record<number, boolean>>({})
const removeLoadingMap = reactive<Record<number, boolean>>({})
const specPopoverId = ref(0)
const productSkuMap = reactive<Record<number, ProductSku[]>>({})

async function loadFavorites() {
  loading.value = true
  try {
    const res = await getFavoriteList()
    favorites.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadProductSkus(productId: number) {
  if (productSkuMap[productId]) return
  try {
    const res = await getProductById(productId)
    productSkuMap[productId] = res.data.skus || []
  } catch { /* ignore */ }
}

async function handleSwitchSku(fav: Favorite, sku: ProductSku) {
  specPopoverId.value = 0
  if (sku.id === fav.skuId) return
  await updateFavoriteSku(fav.productId, sku.id)
  fav.skuId = sku.id
  fav.specDesc = sku.specName + ':' + sku.specValue
  if (sku.price != null) fav.price = sku.price
  if (sku.stock != null) fav.stock = sku.stock
  ElMessage.success('规格已更新')
}

function goDetail(id: number) {
  router.push(`/products/${id}`)
}

async function handleAddToCart(fav: Favorite) {
  addLoadingMap[fav.productId] = true
  try {
    await cartStore.addItem(fav.productId, fav.skuId || 0, 1)
    ElMessage.success('已加入购物车')
  } finally {
    addLoadingMap[fav.productId] = false
  }
}

async function handleRemove(fav: Favorite) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${fav.productName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  removeLoadingMap[fav.productId] = true
  try {
    await removeFavorite(fav.productId)
    ElMessage.success('已取消收藏')
    favorites.value = favorites.value.filter((f) => f.productId !== fav.productId)
  } finally {
    removeLoadingMap[fav.productId] = false
  }
}

onMounted(() => { loadFavorites() })
</script>

<style lang="scss" scoped>
.favorites-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
}

.favorites-content {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.favorites-items {
  padding: 8px 20px;
}

.favorite-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }

  &__img {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
    cursor: pointer;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 15px;
    font-weight: 500;

    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: pointer;
  }

  &__spec {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__price {
    width: 100px;
    font-size: 15px;
    font-weight: 600;
    text-align: center;
    flex-shrink: 0;
  }

  &__actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
}

.spec-popover-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spec-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid #eee;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all .2s;

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
    width: 48px;
    height: 48px;
    border-radius: 4px;
    object-fit: cover;
    flex-shrink: 0;
    border: 1px solid #f0f0f0;
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__name { font-size: 13px; font-weight: 600; color: #333; }
  &__label { font-size: 11px; color: #999; }
  &__price {
    font-size: 12px;
    color: #e6423a;
    font-family: 'SF Mono', monospace;
    flex-shrink: 0;
  }
}

@media (max-width: 768px) {
  .favorite-item {
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;

    &__info { width: calc(100% - 140px); }
    &__price { width: auto; font-size: 14px; }

    &__actions {
      width: 100%;
      justify-content: flex-end;
    }
  }
}
</style>
