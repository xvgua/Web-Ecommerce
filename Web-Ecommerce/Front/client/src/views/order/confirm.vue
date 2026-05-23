<template>
  <div class="order-confirm">
    <h1>确认订单</h1>

    <div class="confirm-section">
      <h2>收货地址</h2>
      <div class="address-list" v-if="addresses.length">
        <div
          v-for="addr in addresses"
          :key="addr.id"
          :class="['address-card', { 'address-card--active': selectedAddressId === addr.id }]"
          @click="selectedAddressId = addr.id"
        >
          <div class="address-card__header">
            <span class="address-card__name">{{ addr.name }}</span>
            <span class="address-card__phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault" size="small" type="primary">默认</el-tag>
          </div>
          <div class="address-card__detail">
            {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无收货地址">
        <el-button type="primary" @click="$router.push('/user/address')">添加地址</el-button>
      </el-empty>
    </div>

    <div class="confirm-section">
      <h2>商品清单</h2>
      <el-table :data="cartStore.checkedItems">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="order-product">
              <div class="order-product__img">
                <ProductImage :src="row.productImage" :seed="row.productName + row.productId" fit="cover" />
              </div>
              <div>
                <div>{{ row.productName }}</div>
                <div class="order-product__spec">{{ row.specDesc }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="80">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">{{ formatPrice(row.price * row.quantity) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="confirm-footer">
      <div class="confirm-footer__total">
        合计：<span class="confirm-footer__price">{{ formatPrice(cartStore.checkedTotal) }}</span>
      </div>
      <el-button type="danger" size="large" :loading="submitting" :disabled="!selectedAddressId" @click="handleSubmit">
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { getAddressList } from '@/api/user'
import { createOrder } from '@/api/order'
import { formatPrice } from '@/utils/format'
import type { Address } from '@shared/types/user'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const cartStore = useCartStore()

const addresses = ref<Address[]>([])
const selectedAddressId = ref<number>(0)
const submitting = ref(false)

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await createOrder({
      addressId: selectedAddressId.value,
      cartItemIds: cartStore.checkedItems.map((item) => item.id),
      remark: '',
    })
    ElMessage.success('订单已提交')
    cartStore.fetchCart()
    router.push(`/orders/${res.data.id}/pay`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getAddressList()
    addresses.value = res.data
    const defaultAddr = addresses.value.find((addr) => addr.isDefault)
    selectedAddressId.value = defaultAddr?.id || addresses.value[0]?.id || 0
  } catch {
    // handled by interceptor
  }
})
</script>

<style lang="scss" scoped>
.order-confirm {
  h1 {
    font-size: 22px;
    margin-bottom: 20px;
  }

  .confirm-section {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 16px;

    h2 {
      font-size: 16px;
      margin-bottom: 16px;
    }
  }

  .address-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .address-card {
    border: 2px solid #eee;
    border-radius: 8px;
    padding: 16px;
    cursor: pointer;
    transition: border-color 0.2s;

    &--active {
      border-color: #409eff;
    }

    &__header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    &__name {
      font-weight: 600;
    }

    &__phone {
      color: #666;
    }

    &__detail {
      font-size: 13px;
      color: #666;
    }
  }

  .order-product {
    display: flex;
    align-items: center;
    gap: 12px;

    &__img {
      width: 50px;
      height: 50px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    &__spec {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }

  .confirm-footer {
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
