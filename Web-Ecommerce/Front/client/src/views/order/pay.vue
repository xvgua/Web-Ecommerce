<template>
  <div class="pay-page">
    <h1 class="page-title">订单支付</h1>

    <div v-loading="loading">
      <el-empty v-if="!loading && !order" description="订单不存在" />

      <template v-if="order">
        <div class="pay-layout">
          <!-- 左侧：订单信息 -->
          <div class="pay-main">
            <!-- 支付倒计时 -->
            <div class="pay-tip" v-if="order.status === 0 && remainingMs > 0">
              <el-icon :size="18"><Clock /></el-icon>
              <span>剩余支付时间 <strong class="pay-countdown">{{ countdown }}</strong></span>
            </div>
            <div class="pay-tip pay-tip--expired" v-else-if="order.status === 0 && expired">
              <el-icon :size="18"><WarningFilled /></el-icon>
              <span>支付超时，订单已取消，商品已退回购物车</span>
            </div>

            <!-- 阶段一：选择支付方式 -->
            <div class="pay-block" v-if="phase === 'select'">
              <h2 class="pay-block__title">选择支付方式</h2>
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

            <!-- 阶段二：展示二维码 -->
            <div class="pay-block" v-if="phase === 'qrcode' || phase === 'scanned'">
              <h2 class="pay-block__title">扫码支付</h2>
              <div class="qr-section">
                <div class="qr-wrapper">
                  <canvas ref="qrCanvasRef" class="qr-canvas"></canvas>
                  <div class="qr-method-badge" :class="'qr-method-badge--' + payIntent?.payMethod">
                    <el-icon v-if="payIntent?.payMethod === 'wechat'" :size="14"><ChatDotSquare /></el-icon>
                    <el-icon v-else-if="payIntent?.payMethod === 'alipay'" :size="14"><Wallet /></el-icon>
                    <el-icon v-else :size="14"><CreditCard /></el-icon>
                    <span>{{ methodLabel }}</span>
                  </div>
                </div>
                <div class="qr-info">
                  <div class="qr-amount">
                    <span class="qr-amount__label">应付金额</span>
                    <span class="qr-amount__value">{{ formatPrice(order.totalAmount) }}</span>
                  </div>
                  <div class="qr-status" v-if="phase === 'qrcode'">
                    <el-icon class="qr-status__icon is-loading" :size="16"><Loading /></el-icon>
                    <span>等待扫码支付...</span>
                  </div>
                  <div class="qr-status qr-status--scanned" v-else-if="phase === 'scanned'">
                    <el-icon class="qr-status__icon" :size="16"><CircleCheckFilled /></el-icon>
                    <span>二维码已扫描，请在手机上确认支付</span>
                  </div>
                  <div class="qr-tip">
                    <span v-if="payIntent?.payMethod === 'wechat'">请使用微信扫一扫</span>
                    <span v-else-if="payIntent?.payMethod === 'alipay'">请使用支付宝扫一扫</span>
                    <span v-else>请使用银行APP扫一扫</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 阶段三：支付状态 -->
            <div class="pay-block pay-block--result" v-if="phase === 'paid'">
              <div class="pay-result">
                <el-icon class="pay-result__icon pay-result__icon--success" :size="48"><CircleCheckFilled /></el-icon>
                <h3 class="pay-result__title">支付成功</h3>
                <p class="pay-result__desc">订单已支付，即将跳转订单详情...</p>
              </div>
            </div>

            <!-- 商品清单 -->
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
                <span>支付方式</span>
                <span>{{ methodLabel || '未选择' }}</span>
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
                <!-- 阶段一按钮 -->
                <template v-if="phase === 'select'">
                  <el-button
                    type="danger"
                    size="large"
                    class="pay-summary__btn"
                    :loading="creatingIntent"
                    @click="handleCreatePayIntent"
                  >
                    立即支付
                  </el-button>
                </template>

                <!-- 阶段二按钮 -->
                <template v-if="phase === 'qrcode' || phase === 'scanned'">
                  <el-button
                    type="danger"
                    size="large"
                    class="pay-summary__btn"
                    :loading="confirming"
                    :disabled="phase !== 'scanned'"
                    @click="handleConfirmPay"
                  >
                    {{ phase === 'scanned' ? '我已完成支付' : '请先扫描二维码' }}
                  </el-button>
                  <el-button
                    size="default"
                    class="pay-summary__back-btn"
                    @click="resetToSelect"
                  >
                    重新选择
                  </el-button>
                </template>
              </div>

              <div class="pay-summary__actions" v-if="phase === 'select'">
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, WarningFilled, ChatDotSquare, Wallet, CreditCard, Loading, CircleCheckFilled } from '@element-plus/icons-vue'
import { getOrderById, cancelOrder, createPayIntent, simulateScan, confirmPay } from '@/api/order'
import { addToCart } from '@/api/cart'
import { formatPrice } from '@/utils/format'
import type { Order, PayIntent } from '@shared/types/order'
import QRCode from 'qrcode'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)
const cancelling = ref(false)
const creatingIntent = ref(false)
const confirming = ref(false)
const selectedMethod = ref('wechat')
const phase = ref<'select' | 'qrcode' | 'scanned' | 'paid'>('select')
const payIntent = ref<PayIntent | null>(null)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)

const PAY_TIMEOUT_MS = 30 * 60 * 1000
const remainingMs = ref(0)
const expired = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let scanTimer: ReturnType<typeof setTimeout> | null = null
let redirectTimer: ReturnType<typeof setTimeout> | null = null

const countdown = computed(() => {
  const ms = remainingMs.value
  if (ms <= 0) return '00:00:00'
  const totalSec = Math.floor(ms / 1000)
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  const cs = Math.floor((ms % 1000) / 10)
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}:${String(cs).padStart(2, '0')}`
})

const methodLabel = computed(() => {
  if (!payIntent.value) {
    const m = payMethods.find(p => p.value === selectedMethod.value)
    return m ? m.name : ''
  }
  const m = payMethods.find(p => p.value === payIntent.value!.payMethod)
  return m ? m.name : ''
})

const payMethods = [
  { value: 'wechat', name: '微信支付', desc: '推荐安装微信用户使用' },
  { value: 'alipay', name: '支付宝', desc: '推荐安装支付宝用户使用' },
  { value: 'card', name: '银行卡支付', desc: '支持储蓄卡及信用卡' },
]

function startCountdown(createTime: string) {
  const deadline = new Date(createTime).getTime() + PAY_TIMEOUT_MS
  countdownTimer = setInterval(() => {
    remainingMs.value = Math.max(0, deadline - Date.now())
    if (remainingMs.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
      handleTimeout()
    }
  }, 10)
}

async function handleTimeout() {
  if (expired.value) return
  expired.value = true
  try {
    await cancelOrder(order.value!.id)
  } catch { /* 订单可能已被取消 */ }
  if (order.value?.items) {
    for (const item of order.value.items) {
      try {
        await addToCart({ productId: item.productId, skuId: item.skuId ?? 0, quantity: item.quantity })
      } catch { /* 个别商品添加失败不阻塞 */ }
    }
  }
}

async function loadOrder() {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data
    if (order.value.status === 0) {
      startCountdown(order.value.createTime)
    } else {
      ElMessage.warning('该订单无需支付')
      router.replace(`/orders/${order.value.id}`)
    }
  } finally {
    loading.value = false
  }
}

async function handleCreatePayIntent() {
  creatingIntent.value = true
  try {
    const res = await createPayIntent(order.value!.id, selectedMethod.value)
    payIntent.value = res.data
    phase.value = 'qrcode'
    await nextTick()
    await renderQRCode()
    // 3 秒后自动模拟扫码
    scanTimer = setTimeout(async () => {
      try {
        await simulateScan(order.value!.id)
        phase.value = 'scanned'
      } catch { /* 模拟扫码失败不阻塞 */ }
    }, 3000)
  } finally {
    creatingIntent.value = false
  }
}

async function renderQRCode() {
  if (!qrCanvasRef.value || !payIntent.value) return
  try {
    const data = JSON.stringify({
      orderNo: payIntent.value.orderNo,
      token: payIntent.value.qrToken,
      amount: payIntent.value.amount,
      method: payIntent.value.payMethod,
    })
    await QRCode.toCanvas(qrCanvasRef.value, data, {
      width: 200,
      margin: 2,
      color: { dark: '#333333', light: '#ffffff' },
    })
  } catch { /* canvas 渲染失败不阻塞 */ }
}

async function handleConfirmPay() {
  confirming.value = true
  try {
    await confirmPay(order.value!.id)
    phase.value = 'paid'
    redirectTimer = setTimeout(() => {
      router.push(`/orders/${order.value!.id}`)
    }, 1500)
  } catch {
    ElMessage.error('支付确认失败，请重试')
  } finally {
    confirming.value = false
  }
}

function resetToSelect() {
  phase.value = 'select'
  payIntent.value = null
  if (scanTimer) {
    clearTimeout(scanTimer)
    scanTimer = null
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
    cancelling.value = false
  }
}

onMounted(() => { loadOrder() })

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (scanTimer) {
    clearTimeout(scanTimer)
    scanTimer = null
  }
  if (redirectTimer) {
    clearTimeout(redirectTimer)
    redirectTimer = null
  }
})
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

  &--expired {
    background: #fef7e0;
    border-color: #fae2b0;
    color: #e6a23c;
  }
}

.pay-countdown {
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
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

  &--result {
    text-align: center;
    padding: 32px 20px;
  }
}

// ── 二维码区域 ──
.qr-section {
  display: flex;
  gap: 24px;
  align-items: center;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.qr-wrapper {
  position: relative;
  width: 200px;
  height: 200px;
  flex-shrink: 0;
  border: 3px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.qr-canvas {
  width: 100%;
  height: 100%;
}

.qr-method-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  font-size: 10px;
  color: #fff;

  &--wechat { background: #07c160; }
  &--alipay { background: #1677ff; }
  &--card   { background: #f56c6c; }
}

.qr-info {
  flex: 1;
  min-width: 0;
}

.qr-amount {
  margin-bottom: 12px;

  &__label {
    display: block;
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
  }

  &__value {
    font-size: 28px;
    font-weight: 700;
    color: #e6423a;
    font-family: 'SF Mono', 'Cascadia Code', monospace;
  }
}

.qr-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;

  &__icon.is-loading {
    animation: rotating 1.5s linear infinite;
  }

  &--scanned {
    color: #07c160;
    font-weight: 600;

    .qr-status__icon {
      color: #07c160;
    }
  }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.qr-tip {
  font-size: 12px;
  color: #999;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}

// ── 支付结果 ──
.pay-result {
  &__icon {
    margin-bottom: 12px;

    &--success {
      color: #07c160;
    }
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }

  &__desc {
    font-size: 13px;
    color: #999;
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

  &__back-btn {
    width: 100%;
    margin-left: 0;
  }

  &__cancel-btn {
    width: 100%;
    font-size: 16px;
    height: 44px;
    margin-left: 0;
  }
}
</style>
