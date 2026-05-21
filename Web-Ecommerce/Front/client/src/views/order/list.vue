<template>
  <div class="order-list-page">
    <h1>我的订单</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
    </el-tabs>

    <div v-loading="loading">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-card__header">
          <span>订单号: {{ order.orderNo }}</span>
          <span class="order-card__time">{{ order.createTime }}</span>
          <el-tag :type="ORDER_STATUS_COLOR[order.status]">{{ order.statusText }}</el-tag>
        </div>
        <div class="order-card__items">
          <div v-for="item in order.items" :key="item.id" class="order-card__item">
            <el-image :src="item.productImage" fit="cover" class="order-card__item-img" />
            <div class="order-card__item-info">
              <div>{{ item.productName }}</div>
              <div class="order-card__item-spec">{{ item.specDesc }}</div>
            </div>
            <div class="order-card__item-price">{{ formatPrice(item.price) }}</div>
            <div class="order-card__item-qty">x{{ item.quantity }}</div>
          </div>
        </div>
        <div class="order-card__footer">
          <span>共 {{ order.items.length }} 件，合计: <strong>{{ formatPrice(order.totalAmount) }}</strong></span>
          <div class="order-card__actions">
            <el-button v-if="order.status === 0" type="primary" size="small" @click="handlePay(order.id)">去支付</el-button>
            <el-button v-if="order.status === 0" size="small" @click="handleCancel(order.id)">取消订单</el-button>
            <el-button v-if="order.status === 2" type="success" size="small" @click="handleConfirm(order.id)">确认收货</el-button>
            <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看详情</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !orders.length" description="暂无订单" />

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 40]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadOrders"
        @size-change="loadOrders"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getOrderList, cancelOrder, confirmReceive } from '@/api/order'
import { formatPrice } from '@/utils/format'
import { ORDER_STATUS_COLOR } from '@shared/constants'
import type { Order } from '@shared/types/order'

const orders = ref<Order[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeTab = ref('')

async function loadOrders() {
  loading.value = true
  try {
    const res = await getOrderList({
      page: page.value,
      pageSize: pageSize.value,
      status: activeTab.value ? Number(activeTab.value) : undefined,
    })
    orders.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  loadOrders()
}

async function handleCancel(id: number) {
  await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  await cancelOrder(id)
  ElMessage.success('订单已取消')
  loadOrders()
}

async function handleConfirm(id: number) {
  await ElMessageBox.confirm('确认已收到商品吗？', '提示', { type: 'warning' })
  await confirmReceive(id)
  ElMessage.success('已确认收货')
  loadOrders()
}

function handlePay(id: number) {
  ElMessage.info('支付功能开发中')
}

onMounted(() => {
  loadOrders()
})
</script>

<style lang="scss" scoped>
.order-list-page {
  h1 {
    font-size: 22px;
    margin-bottom: 20px;
  }

  .order-card {
    background: #fff;
    border-radius: 8px;
    margin-bottom: 16px;
    overflow: hidden;

    &__header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 12px 20px;
      background: #fafafa;
      font-size: 14px;
    }

    &__time {
      color: #999;
      flex: 1;
    }

    &__items {
      padding: 0 20px;
    }

    &__item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      &-img {
        width: 60px;
        height: 60px;
        border-radius: 4px;
        flex-shrink: 0;
      }

      &-info {
        flex: 1;
      }

      &-spec {
        font-size: 12px;
        color: #999;
        margin-top: 4px;
      }

      &-price {
        font-size: 14px;
        font-weight: 600;
      }

      &-qty {
        color: #999;
        font-size: 14px;
      }
    }

    &__footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 20px;
      border-top: 1px solid #f5f5f5;

      strong {
        color: #e6423a;
        font-size: 16px;
      }
    }

    &__actions {
      display: flex;
      gap: 8px;
    }
  }

  .pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: 24px;
  }
}
</style>
