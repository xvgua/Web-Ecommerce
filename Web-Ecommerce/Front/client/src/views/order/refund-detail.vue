<template>
  <div class="refund-detail-page" v-loading="loading">
    <div class="refund-detail" v-if="order">
      <div class="page-header">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <h1 class="page-title">退款详情</h1>
      </div>

      <!-- 退款进度 -->
      <div class="section">
        <h2 class="section-title">退款进度</h2>
        <el-steps :active="stepIndex" finish-status="success" align-center>
          <el-step title="提交申请" :description="formatDate(order.refundApplyTime || '')" />
          <el-step v-if="order.refundStatus !== 3" title="审核中" />
          <el-step title="退款完成" />
        </el-steps>
        <div class="status-badge">
          <el-tag :type="REFUND_STATUS_COLOR[order.refundStatus!]" size="large" effect="dark">
            {{ order.refundStatusText }}
          </el-tag>
          <span v-if="order.refundStatus === 1 && order.refundRejectReason" class="reject-reason">
            拒绝原因：{{ order.refundRejectReason }}
          </span>
        </div>
      </div>

      <!-- 退款信息 -->
      <div class="section">
        <h2 class="section-title">退款信息</h2>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label">退款类型</span>
            <span class="info-value">{{ order.refundType === 1 ? '仅退款' : '退货退款' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">退款金额</span>
            <span class="info-value price">{{ formatPrice(order.refundAmount || 0) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">退款原因</span>
            <span class="info-value">{{ order.refundReasonText }}</span>
          </div>
          <div class="info-row" v-if="order.refundDesc">
            <span class="info-label">补充说明</span>
            <span class="info-value">{{ order.refundDesc }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">申请时间</span>
            <span class="info-value">{{ formatDate(order.refundApplyTime || '') }}</span>
          </div>
          <div class="info-row" v-if="order.refundDealTime">
            <span class="info-label">处理时间</span>
            <span class="info-value">{{ formatDate(order.refundDealTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 退款商品 -->
      <div class="section" v-if="order.refundItems?.length">
        <h2 class="section-title">退款商品</h2>
        <div class="refund-items">
          <div v-for="item in order.refundItems" :key="item.id" class="refund-item">
            <div class="refund-item__img">
              <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
            </div>
            <div class="refund-item__info">
              <div class="refund-item__name">{{ item.productName }}</div>
              <div class="refund-item__spec" v-if="item.specDesc">{{ item.specDesc }}</div>
            </div>
            <div class="refund-item__right">
              <span>{{ formatPrice(item.price) }} x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-row" v-if="order.refundStatus === 0">
        <el-button type="warning" @click="handleCancelRefund">撤销申请</el-button>
      </div>
      <div class="action-row" v-if="order.refundStatus === 1 || order.refundStatus === 3">
        <el-button type="primary" @click="$router.push(`/orders/${order.id}/refund/apply`)">再次申请</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !order" description="退款记录不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getRefundDetail, cancelRefund } from '@/api/order'
import { formatPrice, formatDate } from '@/utils/format'
import { REFUND_STATUS_MAP, REFUND_STATUS_COLOR, REFUND_REASON_MAP } from '@shared/constants'
import type { Order } from '@shared/types/order'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)

const stepIndex = computed(() => {
  if (!order.value) return 0
  const status = order.value.refundStatus!
  if (status === 1 || status === 3) return 1 // rejected or cancelled
  if (status === 0) return 1 // pending review
  if (status === 2) return 2 // completed
  return 0
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getRefundDetail(Number(route.params.id))
    order.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleCancelRefund() {
  try {
    await ElMessageBox.confirm('确定要撤销退款申请吗？', '提示', { type: 'warning' })
    await cancelRefund(order.value!.id)
    ElMessage.success('退款申请已撤销')
    loadDetail()
  } catch { /* cancelled */ }
}

onMounted(() => { loadDetail() })
</script>

<style lang="scss" scoped>
.refund-detail-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
}

.status-badge {
  margin-top: 20px;
  text-align: center;
}

.reject-reason {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  color: #f56c6c;
}

.info-list {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.info-row {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.info-label {
  width: 80px;
  font-size: 13px;
  color: #999;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #333;

  &.price {
    font-weight: 700;
    color: #e6423a;
  }
}

.refund-items {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.refund-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }

  &__img {
    width: 48px;
    height: 48px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__info { flex: 1; min-width: 0; }

  &__name { font-size: 14px; font-weight: 500; }

  &__spec { font-size: 12px; color: #999; margin-top: 2px; }

  &__right { font-size: 13px; color: #666; }
}

.logistics-form {
  display: flex;
  gap: 12px;
  margin-top: 16px;

  .logistics-input {
    width: 200px;
  }
}

.action-row {
  padding-bottom: 40px;
  text-align: center;
}

@media (max-width: 768px) {
  .refund-detail-page {
    padding: 0 8px;
  }

  .section {
    padding: 16px;
  }

  .info-row {
    flex-direction: column;
    gap: 4px;
    padding: 10px 12px;
  }

  .info-label {
    width: auto;
  }
}
</style>
