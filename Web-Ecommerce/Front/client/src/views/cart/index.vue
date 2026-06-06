<template>
  <div class="cart-page">
    <h1 class="page-title">购物车</h1>

    <div class="cart-content" v-if="cartStore.items.length">
      <div class="cart-toolbar">
        <div class="cart-toolbar__left">
          <el-checkbox v-model="checkAll" size="large">
            全选（{{ cartStore.checkedItems.length }}/{{ cartStore.items.length }}）
          </el-checkbox>
        </div>
        <div class="cart-toolbar__right">
          <el-button link size="small" @click="handleCheckInverse">反选</el-button>
          <el-button link size="small" :disabled="!cartStore.checkedItems.length" @click="handleRemoveSelected">删除</el-button>
          <el-button link size="small" :disabled="!cartStore.checkedItems.length" @click="handleBatchFavorite">移入收藏</el-button>
        </div>
      </div>

      <div class="cart-items">
        <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <el-checkbox :model-value="!!item.checked" class="cart-item__check" @change="(val: boolean) => cartStore.updateItem(item.id, item.quantity, val)" />
          <div class="cart-item__img" @click="$router.push(`/products/${item.productId}`)">
            <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
          </div>
          <div class="cart-item__info">
            <div class="cart-item__name" @click="$router.push(`/products/${item.productId}`)">{{ item.productName }}</div>
            <el-popover
              v-if="item.hasSku"
              :visible="specPopoverId === item.id"
              placement="bottom"
              :width="220"
              trigger="click"
              @show="loadProductSkus(item.productId)"
              @hide="specPopoverId = 0"
            >
              <template #reference>
                <div
                  :class="['cart-item__spec-select', { 'is-open': specPopoverId === item.id }]"
                  @click="specPopoverId = specPopoverId === item.id ? 0 : item.id"
                >
                  <span v-if="item.specDesc">{{ item.specDesc }}</span>
                  <span v-else class="cart-item__spec-select--empty">未选择规格</span>
                  <el-icon class="cart-item__spec-arrow"><ArrowDown /></el-icon>
                </div>
              </template>
              <div class="spec-popover-list" v-if="productSkuMap[item.productId]?.length">
                <div
                  v-for="sku in productSkuMap[item.productId]"
                  :key="sku.id"
                  :class="['spec-option', {
                    'spec-option--active': sku.id === item.skuId,
                    'spec-option--disabled': sku.stock === 0
                  }]"
                  @click="sku.stock > 0 && handleSwitchSku(item, sku)"
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
          <div class="cart-item__price">{{ formatPrice(item.price) }}</div>
          <div class="cart-item__qty">
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="item.stock"
              size="default"
              @change="handleQuantityChange(item)"
            />
          </div>
          <div class="cart-item__subtotal">{{ formatPrice(item.price * item.quantity) }}</div>
          <div class="cart-item__remove">
            <el-button type="danger" link @click="handleRemove(item.id)">
              <el-icon size="18"><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="cart-footer">
        <div class="cart-footer__left" />
        <div class="cart-footer__right">
          <span class="cart-footer__label">
            已选 <strong>{{ cartStore.checkedItems.length }}</strong> 件，合计：
          </span>
          <span class="cart-footer__price">{{ formatPrice(cartStore.checkedTotal) }}</span>
          <el-button
            type="danger"
            size="large"
            :disabled="!cartStore.checkedItems.length"
            class="cart-footer__btn"
            @click="handleCheckout"
          >
            去结算
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="购物车还是空的">
      <el-button type="primary" size="large" @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Delete, ArrowDown } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { getProductById } from '@/api/product'
import { batchAddFavorites } from '@/api/favorite'
import { formatPrice } from '@/utils/format'
import type { CartItem } from '@shared/types/cart'
import type { ProductSku } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const cartStore = useCartStore()

const specPopoverId = ref(0)
const productSkuMap = reactive<Record<number, ProductSku[]>>({})

async function loadProductSkus(productId: number) {
  if (productSkuMap[productId]) return
  try {
    const res = await getProductById(productId)
    productSkuMap[productId] = res.data.skus || []
  } catch { /* ignore */ }
}

async function handleSwitchSku(item: CartItem, sku: ProductSku) {
  specPopoverId.value = 0
  if (sku.id === item.skuId) return
  await cartStore.updateItem(item.id, item.quantity, item.checked, sku.id)
}

const checkAll = computed({
  get: () => cartStore.items.length > 0 && cartStore.items.every((item) => item.checked),
  set: (val: boolean) => cartStore.toggleCheckAll(val),
})

function handleQuantityChange(item: CartItem) {
  cartStore.updateItem(item.id, item.quantity, item.checked)
}

async function handleCheckInverse() {
  await cartStore.toggleCheckInverse()
}

async function handleRemove(id: number) {
  await ElMessageBox.confirm('确定要删除该商品吗？', '提示', { type: 'warning' })
  await cartStore.removeItem(id)
}

async function handleRemoveSelected() {
  await ElMessageBox.confirm(`确定要删除选中的 ${cartStore.checkedItems.length} 件商品吗？`, '提示', { type: 'warning' })
  await cartStore.removeSelected()
  ElMessage.success('已删除')
}

async function handleBatchFavorite() {
  const items = cartStore.checkedItems.map((item) => ({
    productId: item.productId,
    skuId: item.skuId,
  }))
  await batchAddFavorites(items)
  await cartStore.removeSelected()
  ElMessage.success(`已将 ${items.length} 件商品移入收藏`)
}

function handleCheckout() {
  router.push('/order/confirm')
}

onMounted(() => { cartStore.fetchCart() })
</script>

<style lang="scss" scoped>
.cart-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
}

.cart-content {
  background: var(--bg1);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: none;
  box-shadow: var(--shadow-sm);
}

.cart-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line-light);

  &__right {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.cart-items {
  padding: 8px 20px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line-light);

  &:last-child { border-bottom: none; }

  &__check { flex-shrink: 0; }

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
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: pointer;
  }

  &__spec-select {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--text3);
    margin-top: 4px;
    padding: 2px 8px;
    border-radius: var(--radius-sm);
    background: var(--bg3);
    cursor: pointer;
    user-select: none;
    transition: all .2s;
    max-width: 100%;

    &:hover {
      background: var(--line-regular);
      color: var(--text1);
    }

    &.is-open {
      background: var(--line-regular);
      color: var(--brand-primary);
    }

    &--empty {
      color: var(--color-warning);
    }

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__spec-arrow {
    font-size: 10px;
    flex-shrink: 0;
    transition: transform .2s;

    .is-open & {
      transform: rotate(180deg);
    }
  }

  &__price {
    width: 100px;
    font-size: 15px;
    font-weight: 600;
    text-align: center;
  }

  &__qty { width: 120px; display: flex; justify-content: center; }

  &__subtotal {
    width: 100px;
    font-size: 15px;
    font-weight: 600;
    color: var(--brand-primary);
    text-align: center;
  }

  &__remove { flex-shrink: 0; }
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  background: var(--bg2);
  border-top: 1px solid var(--line-light);

  &__right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__label {
    font-size: 14px;
    color: var(--text3);
    strong { color: var(--text1); }
  }

  &__price {
    font-size: 24px;
    font-weight: 700;
    color: var(--brand-primary);
    font-family: 'SF Mono', monospace;
  }

  &__btn {
    height: 44px;
    padding: 0 32px;
    font-size: 16px;
    border-radius: var(--radius-md);
    background: var(--brand-primary);
    border: none;
    box-shadow: 2px 2px 6px rgba(160, 135, 110, 0.25);

    &:hover {
      background: var(--brand-primary-hover);
      box-shadow: 3px 3px 8px rgba(160, 135, 110, 0.35);
    }

    &.is-disabled {
      background: var(--bg3);
      box-shadow: none;
    }
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
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  transition: all .2s;

  &:hover {
    border-color: var(--brand-primary);
  }

  &--active {
    border-color: var(--brand-primary);
    background: var(--brand-primary-ghost);
  }

  &--disabled {
    opacity: .45;
    cursor: not-allowed;

    &:hover { border-color: var(--line-light); }
  }

  &__img {
    width: 48px;
    height: 48px;
    border-radius: var(--radius-sm);
    object-fit: cover;
    flex-shrink: 0;
    border: 1px solid var(--line-light);
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--text1);
  }

  &__label {
    font-size: 11px;
    color: var(--text4);
  }

  &__price {
    font-size: 12px;
    color: var(--brand-primary);
    font-family: 'SF Mono', monospace;
    flex-shrink: 0;
  }
}

@media (max-width: 1024px) {
  .cart-page {
    padding: 0 16px;
  }
}

@media (max-width: 768px) {
  .cart-item {
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 0;

    &__info {
      width: calc(100% - 140px);
    }

    &__price,
    &__subtotal {
      width: auto;
      font-size: 14px;
    }

    &__qty { width: auto; }
  }

  .cart-footer {
    flex-direction: column;
    gap: 12px;

    &__right {
      width: 100%;
      justify-content: space-between;
    }

    &__btn { width: 100%; }
  }
}
</style>
