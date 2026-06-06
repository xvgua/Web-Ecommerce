<template>
  <div class="refund-apply-page" v-loading="loading">
    <div class="refund-apply" v-if="order">
      <div class="page-header">
        <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <h1 class="page-title">申请退款</h1>
      </div>

      <div class="order-summary">
        <span>订单号：{{ order.orderNo }}</span>
        <span class="order-amount">实付金额：<strong>{{ formatPrice(order.payAmount || order.totalAmount) }}</strong></span>
      </div>

      <!-- 退款类型 -->
      <div class="section">
        <h2 class="section-title">退款类型</h2>
        <el-radio-group v-model="form.refundType" class="refund-type-group">
          <el-radio :value="1" border>仅退款<span class="radio-hint">（未收到货 / 已协商仅退款）</span></el-radio>
          <el-radio v-if="order.status === 3" :value="2" border>退货退款<span class="radio-hint">（已收到货，需退回商品）</span></el-radio>
        </el-radio-group>
      </div>

      <!-- 退款商品 -->
      <div class="section">
        <h2 class="section-title">退款商品（勾选需退款的商品）</h2>
        <div class="refund-items">
          <div v-for="item in order.items" :key="item.id" class="refund-item" :class="{ selected: form.itemIds.includes(item.id) }" @click="toggleItem(item)">
            <el-checkbox :model-value="form.itemIds.includes(item.id)" @click.stop @change="toggleItem(item)" />
            <div class="refund-item__img">
              <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
            </div>
            <div class="refund-item__info">
              <div class="refund-item__name">{{ item.productName }}</div>
              <div class="refund-item__spec" v-if="item.specDesc">{{ item.specDesc }}</div>
            </div>
            <div class="refund-item__right">
              <span class="refund-item__price">{{ formatPrice(item.price) }}</span>
              <span class="refund-item__qty">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
        <div class="refund-amount" v-if="refundAmount > 0">
          退款金额合计：<strong>{{ formatPrice(refundAmount) }}</strong>
        </div>
      </div>

      <!-- 退款原因 -->
      <div class="section">
        <h2 class="section-title">退款原因</h2>
        <el-select v-model="form.refundReason" placeholder="请选择退款原因" class="reason-select">
          <el-option v-for="(text, val) in REFUND_REASON_MAP" :key="val" :label="text" :value="val" />
        </el-select>
        <el-input
          v-model="form.refundDesc"
          type="textarea"
          :rows="3"
          placeholder="补充说明（选填）"
          maxlength="500"
          show-word-limit
          class="reason-desc"
        />
      </div>

      <div class="submit-row">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !order" description="订单不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getOrderById, refundOrder } from '@/api/order'
import { formatPrice } from '@/utils/format'
import { REFUND_REASON_MAP } from '@shared/constants'
import type { Order } from '@shared/types/order'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)
const submitting = ref(false)
const form = ref({
  refundType: 1,
  refundReason: '',
  refundDesc: '',
  itemIds: [] as number[],
})

const refundAmount = computed(() => {
  if (!order.value) return 0
  return order.value.items
    .filter(it => form.value.itemIds.includes(it.id))
    .reduce((sum, it) => sum + it.price * it.quantity, 0)
})

function toggleItem(item: Order['items'][number]) {
  const idx = form.value.itemIds.indexOf(item.id)
  if (idx > -1) {
    form.value.itemIds.splice(idx, 1)
  } else {
    form.value.itemIds.push(item.id)
    // If refund type is 2 (return) and order isn't completed, reset to refund-only
    if (form.value.refundType === 2 && order.value?.status !== 3) {
      form.value.refundType = 1
    }
  }
}

async function handleSubmit() {
  if (!form.value.refundReason) {
    ElMessage.warning('请选择退款原因')
    return
  }
  if (form.value.itemIds.length === 0) {
    ElMessage.warning('请选择要退款的商品')
    return
  }
  submitting.value = true
  try {
    await refundOrder(order.value!.id, {
      refundType: form.value.refundType,
      refundReason: form.value.refundReason,
      refundDesc: form.value.refundDesc || undefined,
      itemIds: form.value.itemIds,
    })
    ElMessage.success('退款申请已提交')
    router.replace(`/orders/${order.value!.id}/refund`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data
    // Redirect if already has refund
    if (order.value.refundStatus != null && order.value.refundStatus !== 1 && order.value.refundStatus !== 3) {
      ElMessage.warning('该订单已有退款申请在处理中')
      router.replace(`/orders/${order.value.id}/refund`)
      return
    }
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.refund-apply-page {
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

.order-summary {
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;

  strong {
    color: #e6423a;
    font-size: 16px;
  }
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

.refund-type-group {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .el-radio {
    margin-right: 0;
    padding: 12px 16px;
    height: auto;
    width: 100%;
  }

  .radio-hint {
    font-size: 12px;
    color: #999;
    margin-left: 4px;
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
  cursor: pointer;
  transition: background .15s;

  &:last-child { border-bottom: none; }
  &:hover { background: #fafafa; }
  &.selected { background: #ecf5ff; }

  &__img {
    width: 48px;
    height: 48px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__info { flex: 1; min-width: 0; }

  &__name {
    font-size: 14px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__spec {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }

  &__price { font-size: 14px; font-weight: 600; }
  &__qty { font-size: 12px; color: #999; }
}

.refund-amount {
  text-align: right;
  padding: 14px 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
  color: #666;

  strong {
    color: #e6423a;
    font-size: 18px;
    margin-left: 4px;
  }
}

.reason-select {
  width: 100%;
  margin-bottom: 12px;
}

.reason-desc {
  width: 100%;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 0 40px;
}

@media (max-width: 768px) {
  .refund-apply-page {
    padding: 0 8px;
  }

  .order-summary {
    flex-direction: column;
    gap: 8px;
    padding: 14px 16px;
  }

  .section {
    padding: 16px;
  }

  .refund-item {
    padding: 10px 12px;
    gap: 8px;

    &__name {
      font-size: 13px;
    }
  }

  .submit-row {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>
