<template>
  <div class="pay-page">
    <h1 class="page-title">订单支付</h1>

    <div v-loading="loading">
      <el-empty v-if="!loading && !order" description="订单不存在" />

      <template v-if="order">
        <div class="pay-layout">
          <!-- 左侧：订单信息 -->
          <div class="pay-main">
            <!-- 支付状态提示 -->
            <div class="pay-tip" v-if="order.status === 0">
              <el-icon :size="18"><Clock /></el-icon>
              <span>订单将在提交后<strong>30分钟</strong>内自动取消，请尽快完成支付</span>
            </div>

            <div class="pay-block">
              <h2 class="pay-block__title">商品清单</h2>
              <div class="pay-products">
                <div v-for="item in order.items" :key="item.id" class="pay-product">
                  <div class="pay-product__img">
                    <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
                  </div>
                  <div class="pay-product__info">
                    <div class="pay-product__name">{{ item.productName }}</div>
                    <div class="pay-product__spec" v-if="item.specDesc">{{ item.specDesc }}</div>
                  </div>
                  <div class="pay-product__price">{{ formatPrice(item.price) }}</div>
                  <div class="pay-product__qty">x{{ item.quantity }}</div>
                  <div class="pay-product__subtotal">{{ formatPrice(item.price * item.quantity) }}</div>
                </div>
              </div>
            </div>

            <div class="pay-block">
              <h2 class="pay-block__title">支付方式</h2>
              <div class="pay-methods">
                <div
                  v-for="m in payMethods"
                  :key="m.value"
                  :class="['pay-method', `pay-method--${m.value}`, { 'pay-method--active': selectedMethod === m.value }]"
                  @click="selectedMethod = m.value"
                >
                  <div class="pay-method__icon">
                    <el-icon v-if="m.value === 'wechat'" :size="22"><ChatDotSquare /></el-icon>
                    <el-icon v-else-if="m.value === 'alipay'" :size="22"><Wallet /></el-icon>
                    <el-icon v-else :size="22"><CreditCard /></el-icon>
                  </div>
                  <div class="pay-method__info">
                    <div class="pay-method__name">{{ m.name }}</div>
                    <div class="pay-method__desc">{{ m.desc }}</div>
                  </div>
                  <div class="pay-method__radio" :class="{ 'pay-method__radio--checked': selectedMethod === m.value }">
                    <span v-if="selectedMethod === m.value" class="pay-method__dot" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：订单摘要 -->
          <div class="pay-sidebar">
            <div class="pay-summary">
              <h3 class="pay-summary__title">订单摘要</h3>

              <div class="pay-summary__row">
                <span>订单编号</span>
                <span class="pay-summary__no">{{ order.orderNo }}</span>
              </div>
              <div class="pay-summary__row">
                <span>提交时间</span>
                <span>{{ order.createTime }}</span>
              </div>
              <div class="pay-summary__row">
                <span>收货地址</span>
              </div>
              <div class="pay-summary__address" v-if="order.address">
                <div class="pay-summary__addr-user">
                  <strong>{{ order.address.name }}</strong>
                  <span>{{ order.address.phone }}</span>
                </div>
                <div class="pay-summary__addr-detail">
                  {{ order.address.province }}{{ order.address.city }}{{ order.address.district }}
                  {{ order.address.detail }}
                </div>
              </div>

              <el-divider />

              <div class="pay-summary__row pay-summary__row--total">
                <span>应付金额</span>
                <span class="pay-summary__price">{{ formatPrice(order.totalAmount) }}</span>
              </div>

              <div class="pay-summary__actions">
                <el-button
                  type="danger"
                  size="large"
                  class="pay-summary__btn"
                  :loading="paying"
                  :disabled="!selectedMethod"
                  @click="handlePay"
                >
                  立即支付
                </el-button>
                <el-button
                  size="large"
                  class="pay-summary__cancel-btn"
                  :loading="cancelling"
                  @click="handleCancel"
                >
                  取消订单
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, ChatDotSquare, Wallet, CreditCard } from '@element-plus/icons-vue'
import { getOrderById, payOrder, cancelOrder } from '@/api/order'
import { formatPrice } from '@/utils/format'
import type { Order } from '@shared/types/order'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)
const paying = ref(false)
const cancelling = ref(false)
const selectedMethod = ref('wechat')

const payMethods = [
  { value: 'wechat', name: '微信支付', desc: '推荐安装微信用户使用' },
  { value: 'alipay', name: '支付宝', desc: '推荐安装支付宝用户使用' },
  { value: 'card', name: '银行卡支付', desc: '支持储蓄卡及信用卡' },
]

async function loadOrder() {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data
    if (order.value.status !== 0) {
      ElMessage.warning('该订单无需支付')
      router.replace(`/orders/${order.value.id}`)
    }
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  if (!selectedMethod.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  paying.value = true
  try {
    await payOrder(order.value!.id, selectedMethod.value)
    ElMessage.success('支付成功')
    router.push(`/orders/${order.value!.id}`)
  } finally {
    paying.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  cancelling.value = true
  try {
    await cancelOrder(order.value!.id)
    ElMessage.success('订单已取消')
    router.push(`/orders/${order.value!.id}`)
  } finally {
    cancelling.value = ''
  }
}

onMounted(() => { loadOrder() })
</script>

<style lang="scss" scoped>
.pay-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.pay-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.pay-main {
  flex: 1;
  min-width: 0;
}

.pay-sidebar {
  width: 360px;
  flex-shrink: 0;

  @media (max-width: 768px) {
    width: 100%;
  }
}

// ── 支付提示 ──
.pay-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  font-size: 13px;
  color: #e6423a;
  margin-bottom: 16px;

  strong { font-weight: 700; }
}

// ── 通用卡片块 ──
.pay-block {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;

  &__title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f5f5f5;
  }
}

// ── 支付方式 ──
.pay-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pay-method {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 2px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;

  &:hover {
    border-color: #d0d0d0;
    box-shadow: 0 2px 8px rgba(0,0,0,.04);
  }

  &--active {
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64,158,255,.12);
  }

  &__icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: #fff;
  }

  &--wechat &__icon { background: #07c160; }
  &--alipay &__icon { background: #1677ff; }
  &--card   &__icon { background: #f56c6c; }

  &__info { flex: 1; min-width: 0; }

  &__name { font-size: 15px; font-weight: 600; color: #333; }

  &__desc { font-size: 12px; color: #999; margin-top: 3px; }

  &__radio {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    border: 2px solid #dcdcdc;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color .2s;

    &--checked {
      border-color: #409eff;
    }
  }

  &__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #409eff;
  }
}

// ── 商品清单 ──
.pay-products {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.pay-product {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }

  &__img {
    width: 52px;
    height: 52px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__info { flex: 1; min-width: 0; }

  &__name { font-size: 14px; font-weight: 500; }

  &__spec { font-size: 12px; color: #999; margin-top: 2px; }

  &__price { width: 80px; font-size: 14px; font-weight: 600; text-align: center; }

  &__qty { width: 50px; color: #999; text-align: center; }

  &__subtotal {
    width: 90px;
    font-size: 14px;
    font-weight: 600;
    color: #e6423a;
    text-align: right;
  }
}

// ── 订单摘要 ──
.pay-summary {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  position: sticky;
  top: 20px;

  &__title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 18px;
  }

  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    color: #666;
    margin-bottom: 10px;

    &--total {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      margin-top: 8px;
    }
  }

  &__no {
    font-family: 'SF Mono', monospace;
    font-size: 12px;
  }

  &__price {
    font-size: 24px;
    font-weight: 700;
    color: #e6423a;
    font-family: 'SF Mono', monospace;
  }

  &__address {
    background: #fafafa;
    border-radius: 6px;
    padding: 10px 12px;
    margin-bottom: 8px;
  }

  &__addr-user {
    display: flex;
    gap: 10px;
    margin-bottom: 6px;
    font-size: 13px;

    strong { color: #333; }

    span { color: #666; }
  }

  &__addr-detail {
    font-size: 12px;
    color: #888;
    line-height: 1.5;
  }

  &__actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 16px;
  }

  &__btn {
    width: 100%;
    font-size: 16px;
    height: 44px;
  }

  &__cancel-btn {
    width: 100%;
    font-size: 16px;
    height: 44px;
    margin-left: 0;
  }
}
</style>
