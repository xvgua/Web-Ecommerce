<template>
  <div class="coupon-page">
    <div class="coupon-header">
      <h1 class="coupon-header__title">领券中心</h1>
      <p class="coupon-header__subtitle">精选优惠券，限时领取</p>
    </div>

    <div class="coupon-grid" v-loading="loading">
      <div
        v-for="coupon in coupons"
        :key="coupon.id"
        :class="['coupon-card', `coupon-card--type${coupon.type}`, { 'coupon-card--received': coupon.received }]"
      >
        <div class="coupon-card__left">
          <div class="coupon-card__icon">{{ typeIcon[coupon.type] }}</div>
          <div class="coupon-card__type-label">{{ typeLabel[coupon.type] }}</div>
        </div>

        <div class="coupon-card__body">
          <div class="coupon-card__price-row">
            <template v-if="coupon.type === 2">
              <span class="coupon-card__price-value">{{ formatDiscount(coupon.discount) }}</span>
              <span class="coupon-card__price-unit">折</span>
            </template>
            <template v-else-if="coupon.type === 3">
              <span class="coupon-card__price-value coupon-card__price-value--free">免邮</span>
            </template>
            <template v-else>
              <span class="coupon-card__price-symbol">¥</span>
              <span class="coupon-card__price-value">{{ coupon.discount }}</span>
            </template>
          </div>

          <div class="coupon-card__condition">
            {{ coupon.minAmount > 0 ? `满${coupon.minAmount}元可用` : '无门槛' }}
          </div>

          <div class="coupon-card__name">{{ coupon.name }}</div>

          <div class="coupon-card__meta">
            <span class="coupon-card__validity">有效期至 {{ formatEndDate(coupon.endTime) }}</span>
            <span class="coupon-card__remain">仅剩 {{ coupon.remainQty }} 张</span>
          </div>
        </div>

        <div class="coupon-card__action">
          <button
            v-if="coupon.userCouponStatus === 1"
            class="coupon-card__btn coupon-card__btn--used"
            disabled
          >
            已使用
          </button>
          <button
            v-else-if="coupon.received && coupon.userCouponStatus === 0"
            class="coupon-card__btn coupon-card__btn--go"
            @click.stop="handleGoUse(coupon)"
          >
            去使用
          </button>
          <button
            v-else-if="!userStore.isLoggedIn"
            class="coupon-card__btn"
            @click.stop="router.push('/login')"
          >
            登录后领取
          </button>
          <button
            v-else
            class="coupon-card__btn"
            :disabled="receiving[coupon.id]"
            @click.stop="handleReceive(coupon)"
          >
            {{ receiving[coupon.id] ? '领取中...' : '立即领取' }}
          </button>
        </div>

        <div class="coupon-card__stamp">{{ typeLabel[coupon.type] }}</div>
      </div>
    </div>

    <el-empty v-if="!loading && !coupons.length" description="暂无优惠券">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadCoupons"
        @size-change="loadCoupons"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCouponList, receiveCoupon } from '@/api/coupon'
import { useUserStore } from '@/stores/user'
import type { Coupon } from '@shared/types/coupon'

const router = useRouter()
const userStore = useUserStore()
const coupons = ref<Coupon[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const receiving = ref<Record<number, boolean>>({})

const typeIcon: Record<number, string> = { 1: '🎫', 2: '🏷️', 3: '📦' }
const typeLabel: Record<number, string> = { 1: '满减券', 2: '折扣券', 3: '免邮券' }

function formatEndDate(dateStr: string) {
  return dateStr ? dateStr.split(' ')[0] : ''
}

function formatDiscount(discount: number) {
  const value = Math.round(discount * 1000) / 100  // 0.95 → 9.5
  return value % 1 === 0 ? value.toFixed(0) : value.toFixed(1)
}

async function loadCoupons() {
  loading.value = true
  try {
    const res = await getCouponList(page.value, pageSize.value)
    coupons.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleReceive(coupon: Coupon) {
  receiving.value[coupon.id] = true
  try {
    await receiveCoupon(coupon.id)
    coupon.received = true
    coupon.userCouponStatus = 0
    coupon.remainQty = Math.max(0, coupon.remainQty - 1)
    ElMessage.success('领取成功')
  } catch {
    // handled by interceptor
  } finally {
    receiving.value[coupon.id] = false
  }
}

function handleGoUse(coupon: Coupon) {
  const scopeType = coupon.scopeType || 1
  if (scopeType === 1) {
    router.push('/')
  } else if (scopeType === 2 && coupon.scopeIds) {
    try {
      const ids = JSON.parse(coupon.scopeIds)
      if (Array.isArray(ids) && ids.length > 0) {
        router.push(`/products?categoryId=${ids[0]}`)
        return
      }
    } catch { /* fall through */ }
    router.push('/products')
  } else if (scopeType === 3 && coupon.scopeIds) {
    try {
      const ids = JSON.parse(coupon.scopeIds)
      if (Array.isArray(ids) && ids.length > 0) {
        router.push(`/products/${ids[0]}`)
        return
      }
    } catch { /* fall through */ }
    router.push('/products')
  } else {
    router.push('/')
  }
}

onMounted(() => { loadCoupons() })
</script>

<style lang="scss" scoped>
.coupon-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.coupon-header {
  text-align: center;
  padding: 32px 0 24px;

  &__title {
    font-size: 28px;
    font-weight: 700;
    color: #1f1f1f;
    margin: 0 0 8px;
  }

  &__subtitle {
    font-size: 15px;
    color: #909090;
    margin: 0;
  }
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  min-height: 200px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.coupon-card {
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  transition: transform .2s, box-shadow .2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0,0,0,.1);
  }

  &--received {
    opacity: .65;
  }

  &--type1 {
    background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
    border-left: 4px solid #ff4757;
  }

  &--type2 {
    background: linear-gradient(135deg, #fff9f0 0%, #fff 100%);
    border-left: 4px solid #ff9f43;
  }

  &--type3 {
    background: linear-gradient(135deg, #f0fff4 0%, #fff 100%);
    border-left: 4px solid #2ed573;
  }

  &__left {
    width: 80px;
    min-height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    padding: 12px 8px;
    background: rgba(0,0,0,.02);
    border-right: 1px dashed #e8e8e8;
  }

  &__icon {
    font-size: 28px;
    margin-bottom: 4px;
  }

  &__type-label {
    font-size: 11px;
    color: #999;
  }

  &__body {
    flex: 1;
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
  }

  &__price-row {
    display: flex;
    align-items: baseline;
    margin-bottom: 4px;
  }

  &__price-symbol {
    font-size: 16px;
    font-weight: 700;
    color: #ff4757;
    margin-right: 2px;
  }

  &__price-value {
    font-size: 32px;
    font-weight: 800;
    color: #ff4757;
    line-height: 1;

    &--free {
      font-size: 24px;
      color: #2ed573;
    }
  }

  &__price-unit {
    font-size: 14px;
    color: #ff4757;
    font-weight: 600;
    margin-left: 2px;
  }

  &__condition {
    font-size: 13px;
    color: #666;
    margin-bottom: 6px;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: #1f1f1f;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 6px;
  }

  &__meta {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #999;
  }

  &__validity { color: #909090; }
  &__remain { color: #ff4757; }

  &__action {
    display: flex;
    align-items: center;
    padding: 0 16px 0 8px;
    flex-shrink: 0;
  }

  &__btn {
    padding: 8px 20px;
    border-radius: 20px;
    border: none;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    color: #fff;
    background: linear-gradient(135deg, #ff4757, #ff6b81);
    transition: opacity .2s;
    white-space: nowrap;

    &:hover:not(:disabled) { opacity: .85; }
    &:disabled { cursor: not-allowed; }

    &--received {
      background: #d0d0d0;
      color: #999;
    }

    &--go {
      background: #fff;
      color: #e6423a;
      border: 1px solid #e6423a;
    }

    &--used {
      background: #e8e8e8;
      color: #b0b0b0;
    }
  }

  &__stamp {
    position: absolute;
    right: 8px;
    top: 8px;
    font-size: 11px;
    color: rgba(0,0,0,.06);
    font-weight: 800;
    transform: rotate(15deg);
    pointer-events: none;
    letter-spacing: 2px;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
