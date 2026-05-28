<template>
  <div class="cart-page">
    <h1 class="page-title">购物车</h1>

    <div class="cart-content" v-if="cartStore.items.length">
      <div class="cart-select-all">
        <el-checkbox v-model="checkAll" size="large">
          全选（{{ cartStore.checkedItems.length }}/{{ cartStore.items.length }}）
        </el-checkbox>
      </div>

      <div class="cart-items">
        <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <el-checkbox v-model="item.checked" class="cart-item__check" @change="handleCheckedChange(item)" />
          <div class="cart-item__img" @click="$router.push(`/products/${item.productId}`)">
            <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
          </div>
          <div class="cart-item__info">
            <div class="cart-item__name" @click="$router.push(`/products/${item.productId}`)">{{ item.productName }}</div>
            <div class="cart-item__spec" v-if="item.specDesc">
              <span>{{ item.specDesc }}</span>
              <el-popover
                :visible="specPopoverId === item.id"
                placement="bottom"
                :width="220"
                trigger="click"
                @show="loadProductSkus(item.productId)"
                @hide="specPopoverId = 0"
              >
                <template #reference>
                  <el-button text size="small" type="primary" @click="specPopoverId = specPopoverId === item.id ? 0 : item.id">
                    换规格
                  </el-button>
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
        <div class="cart-footer__left">
          <el-button link @click="cartStore.clearAll">清空购物车</el-button>
        </div>
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
import { ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { getProductById } from '@/api/product'
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

function handleCheckedChange(item: CartItem) {
  cartStore.updateItem(item.id, item.quantity, item.checked)
}

async function handleRemove(id: number) {
  await ElMessageBox.confirm('确定要删除该商品吗？', '提示', { type: 'warning' })
  cartStore.removeItem(id)
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
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.cart-select-all {
  padding: 16px 20px 0;
}

.cart-items {
  padding: 8px 20px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;

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
    cursor: pointer;
  }

  &__name {
    font-size: 15px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
  }

  &__qty { width: 120px; display: flex; justify-content: center; }

  &__subtotal {
    width: 100px;
    font-size: 15px;
    font-weight: 600;
    color: #e6423a;
    text-align: center;
  }

  &__remove { flex-shrink: 0; }
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;

  &__right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__label {
    font-size: 14px;
    color: #666;
    strong { color: #333; }
  }

  &__price {
    font-size: 24px;
    font-weight: 700;
    color: #e6423a;
    font-family: 'SF Mono', monospace;
  }

  &__btn {
    height: 44px;
    padding: 0 32px;
    font-size: 16px;
    border-radius: 10px;
    background: linear-gradient(135deg, #ff6f3f, #e6423a);
    border: none;

    &:hover { background: linear-gradient(135deg, #e85d2f, #d63a32); }

    &.is-disabled {
      background: #e0e0e0;
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
  border: 1px solid #eee;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all .2s;

  &:hover {
    border-color: #409eff;
  }

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

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: #333;
  }

  &__label {
    font-size: 11px;
    color: #999;
  }

  &__price {
    font-size: 12px;
    color: #e6423a;
    font-family: 'SF Mono', monospace;
    flex-shrink: 0;
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
