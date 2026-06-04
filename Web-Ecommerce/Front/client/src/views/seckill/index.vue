<template>
  <div class="seckill-page">
    <!-- Header with countdown -->
    <div class="seckill-header" v-if="currentActivity">
      <div class="seckill-header__info">
        <h2 class="seckill-header__title">
          <el-icon :size="24"><Timer /></el-icon>
          {{ currentActivity.name }}
        </h2>
        <div class="seckill-header__countdown">
          <span class="countdown-label">{{ countdownLabel }}</span>
          <span class="countdown-timer">{{ countdownText }}</span>
        </div>
      </div>
    </div>

    <!-- Activity tabs for multiple concurrent activities -->
    <div class="seckill-tabs" v-if="activities.length > 1">
      <div
        v-for="act in activities"
        :key="act.id"
        :class="['seckill-tab', { active: currentActivity?.id === act.id }]"
        @click="selectActivity(act)"
      >
        {{ act.name }}
      </div>
    </div>

    <!-- Product grid -->
    <div class="seckill-products" v-if="currentActivity && currentActivity.products?.length">
      <div
        v-for="sp in currentActivity.products"
        :key="sp.id"
        class="seckill-product-card"
      >
        <div class="seckill-product-card__image" @click="goProduct(sp.productId)">
          <ProductImage :src="sp.productImage" :seed="sp.productName + sp.productId" fit="cover" />
          <div class="seckill-tag" v-if="sp.remainStock === 0">已售罄</div>
        </div>
        <div class="seckill-product-card__info">
          <h4 class="product-name" @click="goProduct(sp.productId)">{{ sp.productName }}</h4>
          <p class="spec-desc" v-if="sp.specDesc">{{ sp.specDesc }}</p>
          <div class="price-row">
            <span class="seckill-price">&yen;{{ sp.seckillPrice }}</span>
            <span class="original-price" v-if="sp.originalPrice">&yen;{{ sp.originalPrice }}</span>
          </div>
          <div class="stock-bar">
            <div class="stock-bar__inner" :style="{ width: stockPercent(sp) + '%' }"></div>
          </div>
          <div class="stock-info">
            <span>已抢{{ sp.seckillStock - sp.remainStock }}件</span>
            <span>剩余{{ sp.remainStock }}件</span>
          </div>
          <el-button
            type="danger"
            :disabled="sp.remainStock === 0 || hasPurchased(sp)"
            @click="goSeckill(sp)"
            class="seckill-btn"
            :loading="seckillingId === sp.id"
          >
            {{ sp.remainStock === 0 ? '已售罄' : hasPurchased(sp) ? '您已抢购过该商品啦' : '立即秒杀' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <el-empty
      v-if="!currentActivity || !currentActivity.products?.length"
      description="暂无秒杀活动"
      :image-size="160"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Timer } from '@element-plus/icons-vue'
import ProductImage from '@/components/common/ProductImage.vue'
import { getActiveActivities, getMyPurchasedSeckill } from '@/api/seckill'
import type { SeckillActivity, SeckillProduct } from '@shared/types/seckill'

const router = useRouter()

const activities = ref<SeckillActivity[]>([])
const currentActivity = ref<SeckillActivity | null>(null)
const now = ref(Date.now())
let timer: number | null = null

const purchasedIds = ref<Set<number>>(new Set())
const seckillingId = ref<number | null>(null)
const hasPurchased = (sp: SeckillProduct) => purchasedIds.value.has(sp.id)

const countdownLabel = computed(() => {
  if (!currentActivity.value) return ''
  const start = new Date(currentActivity.value.startTime).getTime()
  const end = new Date(currentActivity.value.endTime).getTime()
  if (now.value < start) return '距离开始'
  if (now.value < end) return '距离结束'
  return '已结束'
})

const countdownText = computed(() => {
  if (!currentActivity.value) return '--:--:--:----'
  const start = new Date(currentActivity.value.startTime).getTime()
  const end = new Date(currentActivity.value.endTime).getTime()
  let diff: number
  if (now.value < start) diff = start - now.value
  else if (now.value < end) diff = end - now.value
  else return '00:00:00:0000'

  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  const ms = diff % 1000
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}:${String(ms).padStart(4, '0')}`
})

function stockPercent(sp: SeckillProduct) {
  if (sp.seckillStock === 0) return 0
  return Math.round((sp.remainStock / sp.seckillStock) * 100)
}

function selectActivity(act: SeckillActivity) {
  currentActivity.value = act
}

function goProduct(productId: number) {
  router.push(`/products/${productId}`)
}

async function loadActivities() {
  try {
    const [actRes, purchasedRes] = await Promise.all([
      getActiveActivities(),
      getMyPurchasedSeckill().catch(() => ({ data: [] as number[] })),
    ])
    activities.value = actRes.data || []
    purchasedIds.value = new Set(purchasedRes.data || [])
    if (activities.value.length > 0) {
      currentActivity.value = activities.value[0]
    }
  } catch {
    // handled by interceptor
  }
}

function goSeckill(sp: SeckillProduct) {
  seckillingId.value = sp.id
  router.push(`/order/confirm?seckillProductId=${sp.id}`)
  seckillingId.value = null
}

onMounted(() => {
  loadActivities()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 50)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.seckill-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.seckill-header {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  border-radius: 12px;
  padding: 24px 32px;
  margin-bottom: 24px;
  color: #fff;
}

.seckill-header__info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.seckill-header__title {
  margin: 0;
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.seckill-header__countdown {
  display: flex;
  align-items: center;
  gap: 12px;
}

.countdown-label {
  font-size: 14px;
  opacity: 0.85;
}

.countdown-timer {
  font-size: 28px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

.seckill-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.seckill-tab {
  padding: 8px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.seckill-tab.active {
  background: #ff4d4f;
  color: #fff;
  border-color: #ff4d4f;
}

.seckill-products {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1024px) {
  .seckill-products {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .seckill-products {
    grid-template-columns: repeat(2, 1fr);
  }
  .seckill-header__info {
    flex-direction: column;
    align-items: flex-start;
  }
}

.seckill-product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  transition: box-shadow 0.3s;
}

.seckill-product-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.seckill-product-card__image {
  position: relative;
  width: 100%;
  height: 200px;
  cursor: pointer;
  overflow: hidden;
  background: #f5f5f5;
}

.seckill-product-card__image :deep(.product-image),
.seckill-product-card__image :deep(.el-image),
.seckill-product-card__image :deep(img) {
  width: 100%;
  height: 100%;
}

.seckill-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.seckill-product-card__info {
  padding: 12px;
}

.product-name {
  margin: 0 0 4px;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-name:hover {
  color: #ff4d4f;
}

.spec-desc {
  margin: 0 0 8px;
  font-size: 12px;
  color: #999;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.seckill-price {
  color: #ff4d4f;
  font-size: 18px;
  font-weight: 700;
}

.original-price {
  color: #999;
  font-size: 13px;
  text-decoration: line-through;
}

.stock-bar {
  height: 6px;
  background: #ffe0e0;
  border-radius: 3px;
  margin-bottom: 6px;
  overflow: hidden;
}

.stock-bar__inner {
  height: 100%;
  background: #ff4d4f;
  border-radius: 3px;
  transition: width 0.3s;
}

.stock-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.seckill-btn {
  width: 100%;
}

</style>
