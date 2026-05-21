<template>
  <div class="order-detail-page" v-loading="loading">
    <h1 class="page-title">订单详情</h1>

    <div class="detail-section" v-if="order">
      <el-card>
        <template #header>订单信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ order.userId }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="ORDER_STATUS_COLOR[order.status]">{{ order.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">{{ formatPrice(order.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <div class="detail-section" v-if="order?.address">
      <el-card>
        <template #header>收货地址</template>
        <p>{{ order.address.name }} {{ order.address.phone }}</p>
        <p>{{ order.address.province }}{{ order.address.city }}{{ order.address.district }} {{ order.address.detail }}</p>
      </el-card>
    </div>

    <div class="detail-section" v-if="order">
      <el-card>
        <template #header>商品清单</template>
        <el-table :data="order.items" border>
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 8px">
                <el-image :src="row.productImage" fit="cover" style="width: 40px; height: 40px; border-radius: 4px" />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="规格" width="120">
            <template #default="{ row }">{{ row.specDesc }}</template>
          </el-table-column>
          <el-table-column label="单价" width="100">
            <template #default="{ row }">{{ formatPrice(row.price) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="80">
            <template #default="{ row }">{{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">{{ formatPrice(row.price * row.quantity) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-empty v-if="!loading && !order" description="订单不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderById } from '@/api/admin'
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
.detail-section {
  margin-bottom: 16px;
}
</style>
