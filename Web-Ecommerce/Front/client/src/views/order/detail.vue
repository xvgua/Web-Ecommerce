<template>
  <div class="order-detail-page" v-loading="loading">
    <div class="order-detail" v-if="order">
      <h1 class="page-title">订单详情</h1>

      <div class="detail-block">
        <div class="detail-block__header">
          <h2>订单信息</h2>
          <el-tag :type="ORDER_STATUS_COLOR[order.status]" effect="dark">{{ order.statusText }}</el-tag>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '---' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="highlight-price">{{ formatPrice(order.totalAmount) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-block" v-if="order.address">
        <h2 class="detail-block__title">收货信息</h2>
        <div class="address-box">
          <span class="address-box__name">{{ order.address.name }}</span>
          <span class="address-box__phone">{{ order.address.phone }}</span>
          <div class="address-box__detail">
            {{ order.address.province }}{{ order.address.city }}{{ order.address.district }}
            {{ order.address.detail }}
          </div>
        </div>
      </div>

      <div class="detail-block">
        <h2 class="detail-block__title">商品清单</h2>
        <div class="product-list">
          <div v-for="item in order.items" :key="item.id" class="product-item">
            <div class="product-item__img">
              <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
            </div>
            <div class="product-item__info">
              <div class="product-item__name">{{ item.productName }}</div>
              <div class="product-item__spec" v-if="item.specDesc">{{ item.specDesc }}</div>
            </div>
            <div class="product-item__price">{{ formatPrice(item.price) }}</div>
            <div class="product-item__qty">x{{ item.quantity }}</div>
            <div class="product-item__subtotal">{{ formatPrice(item.price * item.quantity) }}</div>
            <div class="product-item__action" v-if="order.status === 3">
              <el-button size="small" type="warning" @click.stop="$router.push(`/orders/${order.id}/review/${item.productId}`)">
                评价
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !order" description="订单不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderById } from '@/api/order'
import { formatPrice } from '@/utils/format'
import { ORDER_STATUS_COLOR } from '@shared/constants'
import type { Order } from '@shared/types/order'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const order = ref<Order | null>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.order-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.detail-block {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { font-size: 16px; font-weight: 600; }
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 14px;
  }
}

.highlight-price {
  color: #e6423a;
  font-size: 18px;
  font-weight: 700;
  font-family: 'SF Mono', monospace;
}

.address-box {
  &__name { font-weight: 600; margin-right: 12px; }
  &__phone { color: #666; }
  &__detail { font-size: 13px; color: #888; margin-top: 6px; }
}

.product-list {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }

  &__img {
    width: 56px;
    height: 56px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__info { flex: 1; min-width: 0; }

  &__name { font-size: 14px; font-weight: 500; }

  &__spec { font-size: 12px; color: #999; margin-top: 2px; }

  &__price { width: 90px; font-size: 14px; font-weight: 600; text-align: center; }

  &__qty { width: 50px; color: #999; text-align: center; }

  &__subtotal {
    width: 100px;
    font-size: 14px;
    font-weight: 600;
    color: #e6423a;
    text-align: right;
  }

  &__action {
    width: 70px;
    text-align: right;
  }
}
</style>
