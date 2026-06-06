<template>
  <div class="order-detail-page" v-loading="loading">
    <div class="order-detail" v-if="order">
      <div class="page-title-row">
        <h1 class="page-title">{{ order.statusText }}</h1>
        <el-button v-if="canRefund" size="small" @click="handleRefund">{{ refundButtonText }}</el-button>
        <el-button v-if="order.status === 3" size="small" @click="handleReorder">再来一单</el-button>
        <el-button v-if="order.status === 4" size="small" @click="handleReorder">加入购物车</el-button>
      </div>

      <div class="detail-block" v-if="order.address">
        <div class="detail-block__header">
          <h2 class="detail-block__title">收货信息</h2>
          <el-button v-if="order.status === 1" size="small" @click="handleEditAddress">修改地址</el-button>
        </div>
        <div class="address-box">
          <div class="address-box__region">
            {{ order.address.province }} {{ order.address.city }} {{ order.address.district }}
          </div>
          <div class="address-box__detail">{{ order.address.detail }}</div>
          <div class="address-box__contact">
            <span class="address-box__name">{{ order.address.name }}</span>
            <span class="address-box__phone">{{ order.address.phone }}</span>
          </div>
        </div>
      </div>

      <div class="detail-block" v-if="order.status >= 1 && order.status <= 3">
        <LogisticsTracker
          :status="order.status"
          :pay-time="order.payTime"
          :deal-time="order.dealTime"
        />
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
            <div class="product-item__action" v-if="order.status === 3 && !item.isReviewed && reviewOpen">
              <el-button size="small" type="warning" @click.stop="$router.push(`/orders/${order.id}/review/${item.productId}`)">
                评价
              </el-button>
            </div>
            <div class="product-item__action" v-if="order.status === 3 && item.isReviewed && !item.hasFollowUp && reviewOpen">
              <el-button size="small" type="warning" @click.stop="$router.push(`/orders/${order.id}/review/${item.productId}?followUp=1`)">
                追加评价
              </el-button>
            </div>
          </div>
        </div>
        <div class="product-list__footer">
          <span>共 {{ order.items.length }} 件商品，合计：</span>
          <strong>{{ formatPrice(order.payAmount || order.totalAmount) }}</strong>
          <span v-if="order.couponDiscount && order.couponDiscount > 0" class="coupon-hint">
            （{{ order.couponName || '优惠券' }}：-{{ formatPrice(order.couponDiscount) }}）
          </span>
        </div>
      </div>

      <div class="detail-block">
        <h2 class="detail-block__title">订单信息</h2>
        <div class="info-list">
          <div class="info-list__row">
            <span class="info-list__label">订单号</span>
            <span class="info-list__value">{{ order.orderNo }}</span>
          </div>
          <div class="info-list__row">
            <span class="info-list__label">创建时间</span>
            <span class="info-list__value">{{ formatDate(order.createTime) }}</span>
          </div>
          <div class="info-list__row" v-if="order.payMethod">
            <span class="info-list__label">支付方式</span>
            <span class="info-list__value">{{ order.payMethod }}</span>
          </div>
          <div class="info-list__row" v-if="order.payTime">
            <span class="info-list__label">支付时间</span>
            <span class="info-list__value">{{ formatDate(order.payTime) }}</span>
          </div>
          <div class="info-list__row" v-if="order.shipTime">
            <span class="info-list__label">发货时间</span>
            <span class="info-list__value">{{ formatDate(order.shipTime) }}</span>
          </div>
          <div class="info-list__row" v-if="order.dealTime">
            <span class="info-list__label">成交时间</span>
            <span class="info-list__value">{{ formatDate(order.dealTime) }}</span>
          </div>
          <div class="info-list__row">
            <span class="info-list__label">商品合计</span>
            <span class="info-list__value">{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <div class="info-list__row" v-if="order.couponDiscount && order.couponDiscount > 0">
            <span class="info-list__label">优惠券抵扣</span>
            <span class="info-list__value" style="color:#e6423a">-{{ formatPrice(order.couponDiscount) }}</span>
          </div>
          <div class="info-list__row">
            <span class="info-list__label">实付金额</span>
            <span class="info-list__value" style="font-weight:700;color:#e6423a">{{ formatPrice(order.payAmount || order.totalAmount) }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !order" description="订单不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderById, reorderOrder } from '@/api/order'
import { formatPrice, formatDate } from '@/utils/format'
import { ORDER_STATUS_COLOR } from '@shared/constants'
import type { Order } from '@shared/types/order'
import ProductImage from '@/components/common/ProductImage.vue'
import LogisticsTracker from '@/components/business/LogisticsTracker.vue'
const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const loading = ref(false)

function handleEditAddress() {
  if (order.value?.addressModified) {
    ElMessage.warning('您已经修改过地址啦')
    return
  }
  router.push(`/orders/${order.value!.id}/edit-address`)
}

const canRefund = computed(() => {
  if (!order.value) return false
  const status = order.value.status
  if (status !== 1 && status !== 2 && status !== 3) return false
  if (order.value.refundStatus != null && order.value.refundStatus !== 1 && order.value.refundStatus !== 3) return false
  return true
})

const refundButtonText = computed(() => {
  if (order.value?.refundStatus === 1 || order.value?.refundStatus === 3) return '再次申请'
  return '申请退款'
})

function handleRefund() {
  const o = order.value!
  if (o.refundStatus != null && o.refundStatus !== 1 && o.refundStatus !== 3) {
    router.push(`/orders/${o.id}/refund`)
  } else {
    router.push(`/orders/${o.id}/refund/apply`)
  }
}

async function handleReorder() {
  const res = await reorderOrder(order.value!.id)
  if (res.data) {
    ElMessage.success('下单成功，即将跳转支付页面')
    router.push(`/orders/${res.data.id}/pay`)
  } else {
    ElMessage.warning('部分商品库存不足，已加入购物车')
    router.push('/cart')
  }
}

const reviewOpen = computed(() => {
  if (!order.value?.dealTime) return true
  const deadline = new Date(order.value.dealTime)
  deadline.setMonth(deadline.getMonth() + 1)
  return deadline > new Date()
})

async function loadOrder() {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadOrder() })
</script>

<style lang="scss" scoped>
.order-detail-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.detail-block {
  background: var(--bg1);
  border-radius: var(--radius-sm);
  padding: 24px;
  margin-bottom: 16px;
  border: 1px solid var(--line-light);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 0;
  }
}

.address-box {
  &__region { font-size: 13px; color: #999; }
  &__detail { font-size: 15px; font-weight: 600; color: #333; margin: 6px 0; }
  &__contact { font-size: 13px; color: #333; }
  &__phone { margin-left: 8px; }
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

.info-list {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  &__row {
    display: flex;
    align-items: center;
    padding: 12px 0 0 12px;
    border-bottom: 1px solid #f5f5f5;

    &:last-child { border-bottom: none; }
  }

  &__label {
    width: 80px;
    font-size: 13px;
    color: #999;
    flex-shrink: 0;
  }

  &__value {
    font-size: 14px;
    color: #333;

    &--price {
      font-size: 18px;
      font-weight: 700;
      color: #e6423a;
    }
  }
}

.product-list__footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 14px 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #666;

  strong {
    font-size: 18px;
    font-weight: 700;
    color: #e6423a;
    margin-left: 4px;
  }
}
</style>
