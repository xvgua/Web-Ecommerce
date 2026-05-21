<template>
  <div class="order-detail-page" v-loading="loading">
    <div class="order-detail" v-if="order">
      <h1>订单详情</h1>

      <div class="detail-section">
        <h2>订单信息</h2>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="ORDER_STATUS_COLOR[order.status]">{{ order.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">{{ formatPrice(order.totalAmount) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-section">
        <h2>收货地址</h2>
        <p v-if="order.address">
          {{ order.address.name }} {{ order.address.phone }}
          {{ order.address.province }}{{ order.address.city }}{{ order.address.district }}
          {{ order.address.detail }}
        </p>
      </div>

      <div class="detail-section">
        <h2>商品清单</h2>
        <el-table :data="order.items">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="order-product">
                <el-image :src="row.productImage" fit="cover" class="order-product__img" />
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
  h1 {
    font-size: 22px;
    margin-bottom: 20px;
  }

  .detail-section {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 16px;

    h2 {
      font-size: 16px;
      margin-bottom: 16px;
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
}
</style>
