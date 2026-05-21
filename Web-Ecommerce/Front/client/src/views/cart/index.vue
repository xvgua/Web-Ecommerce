<template>
  <div class="cart-page">
    <h1>购物车</h1>

    <div class="cart-table" v-if="cartStore.items.length">
      <el-checkbox v-model="checkAll" @change="cartStore.toggleCheckAll">全选</el-checkbox>
      <el-table :data="cartStore.items" style="margin-top: 12px">
        <el-table-column width="50">
          <template #default="{ row }">
            <el-checkbox v-model="row.checked" />
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="cart-product">
              <el-image :src="row.productImage" fit="cover" class="cart-product__img" />
              <div>
                <div>{{ row.productName }}</div>
                <div class="cart-product__spec">{{ row.specDesc }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="row.stock"
              size="small"
              @change="handleQuantityChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">{{ formatPrice(row.price * row.quantity) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" text @click="handleRemove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-else description="购物车为空">
      <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>

    <div class="cart-footer" v-if="cartStore.items.length">
      <div class="cart-footer__total">
        已选 {{ cartStore.checkedItems.length }} 件，合计：
        <span class="cart-footer__price">{{ formatPrice(cartStore.checkedTotal) }}</span>
      </div>
      <el-button type="danger" size="large" :disabled="!cartStore.checkedItems.length" @click="handleCheckout">
        去结算
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'
import type { CartItem } from '@shared/types/cart'

const router = useRouter()
const cartStore = useCartStore()

const checkAll = computed({
  get: () => cartStore.items.every((item) => item.checked),
  set: (val: boolean) => cartStore.toggleCheckAll(val),
})

function handleQuantityChange(item: CartItem) {
  cartStore.updateItem(item.id, item.quantity, item.checked)
}

async function handleRemove(id: number) {
  await ElMessageBox.confirm('确定要删除该商品吗？', '提示', { type: 'warning' })
  cartStore.removeItem(id)
}

function handleCheckout() {
  router.push('/order/confirm')
}

onMounted(() => {
  cartStore.fetchCart()
})
</script>

<style lang="scss" scoped>
.cart-page {
  h1 {
    font-size: 22px;
    margin-bottom: 20px;
  }

  .cart-table {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
  }

  .cart-product {
    display: flex;
    align-items: center;
    gap: 12px;

    &__img {
      width: 60px;
      height: 60px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    &__spec {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }

  .cart-footer {
    position: sticky;
    bottom: 0;
    background: #fff;
    margin-top: 20px;
    padding: 16px 20px;
    border-radius: 8px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 20px;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);

    &__total {
      font-size: 14px;
    }

    &__price {
      font-size: 22px;
      font-weight: 700;
      color: #e6423a;
    }
  }
}
</style>
