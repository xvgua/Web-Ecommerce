<template>
  <div class="seckill-page">
    <div class="seckill-page__header">
      <h1 class="seckill-page__title">
        <el-icon :size="26"><Timer /></el-icon>
        限时秒杀
      </h1>
    </div>

    <!-- Empty state -->
    <el-empty
      v-if="!activities.length"
      description="暂无秒杀活动"
      :image-size="160"
    />

    <!-- Activity sections -->
    <section
      v-for="act in activities"
      :key="act.id"
      class="seckill-section"
      :class="{ 'seckill-section--upcoming': act.status === 0 }"
      :style="act.backgroundImage
        ? { background: `url(${act.backgroundImage}) center/cover no-repeat` }
        : {}"
    >
      <div class="seckill-section__bar">
        <div class="seckill-section__bar-left">
          <h2 class="seckill-section__name">{{ act.name }}</h2>
          <span v-if="act.status === 1" class="seckill-section__badge seckill-section__badge--active">进行中</span>
          <span v-else class="seckill-section__badge seckill-section__badge--upcoming">即将开始</span>
        </div>
        <div class="seckill-section__bar-right">
          <template v-if="act.status === 1">
            <span class="countdown-label">距离结束</span>
            <span class="countdown-timer">{{ countdownText(act, 'end') }}</span>
          </template>
          <template v-else>
            <span class="countdown-label">开始时间</span>
            <span class="seckill-section__start-time">{{ formatStartTime(act.startTime) }}</span>
          </template>
        </div>
      </div>

      <!-- Products -->
      <div class="seckill-products" v-if="act.products?.length">
        <div
          v-for="sp in act.products"
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
              :disabled="sp.remainStock === 0 || hasPurchased(sp) || act.status === 0"
              @click="goSeckill(sp)"
              class="seckill-btn"
              :loading="seckillingId === sp.id"
            >
              <template v-if="act.status === 0">即将开始</template>
              <template v-else-if="sp.remainStock === 0">已售罄</template>
              <template v-else-if="hasPurchased(sp)">已抢购</template>
              <template v-else>立即秒杀</template>
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无秒杀商品" :image-size="80" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Timer } from '@element-plus/icons-vue'
import ProductImage from '@/components/common/ProductImage.vue'
import { getAllActivities, getMyPurchasedSeckill } from '@/api/seckill'
import type { SeckillActivity, SeckillProduct } from '@shared/types/seckill'

const router = useRouter()

const activities = ref<SeckillActivity[]>([])
const now = ref(Date.now())
let timer: number | null = null

const purchasedIds = ref<Set<number>>(new Set())
const seckillingId = ref<number | null>(null)
const hasPurchased = (sp: SeckillProduct) => purchasedIds.value.has(sp.id)

function countdownText(act: SeckillActivity, type: 'end' | 'start'): string {
  const target = type === 'end'
    ? new Date(act.endTime).getTime()
    : new Date(act.startTime).getTime()
  let diff = target - now.value
  if (diff < 0) return '00:00:00'

  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function formatStartTime(dateStr: string): string {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hours = String(d.getHours()).padStart(2, '0')
  const mins = String(d.getMinutes()).padStart(2, '0')
  return `${month}月${day}日 ${hours}:${mins}`
}

function stockPercent(sp: SeckillProduct) {
  if (sp.seckillStock === 0) return 0
  return Math.round((sp.remainStock / sp.seckillStock) * 100)
}

function goProduct(productId: number) {
  router.push(`/products/${productId}`)
}

async function loadActivities() {
  try {
    const [actRes, purchasedRes] = await Promise.all([
      getAllActivities(),
      getMyPurchasedSeckill().catch(() => ({ data: [] as number[] })),
    ])
    activities.value = actRes.data || []
    purchasedIds.value = new Set(purchasedRes.data || [])
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
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.seckill-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.seckill-page__header {
  margin-bottom: 24px;
}

.seckill-page__title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text1);
}

/* ── Activity section ── */
.seckill-section {
  margin-bottom: 32px;
  background: var(--bg1);
  border-radius: var(--radius-sm);
  overflow: hidden;

  &--upcoming {
    .seckill-section__bar {
      background: linear-gradient(135deg, #f0f0f0 0%, #e8e8e8 100%);
    }
  }
}

.seckill-section__bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  border-radius: 10px 10px 0 0;
  color: #fff;
  flex-wrap: wrap;
  gap: 12px;
}

.seckill-section__bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seckill-section__name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.seckill-section__badge {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 600;

  &--active {
    background: rgba(255, 255, 255, 0.25);
  }

  &--upcoming {
    background: rgba(0, 0, 0, 0.1);
    color: #666;
  }
}

.seckill-section__bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.seckill-section__start-time {
  font-size: 16px;
  font-weight: 600;
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
}

.countdown-label {
  font-size: 13px;
  opacity: 0.85;
}

.countdown-timer {
  font-size: 22px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

/* ── Products grid ── */
.seckill-products {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #eee;
  border: 1px solid #eee;
  border-top: none;
  border-radius: 0 0 10px 10px;
  overflow: hidden;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.seckill-product-card {
  background: #fff;

  &__image {
    position: relative;
    width: 100%;
    aspect-ratio: 1;
    cursor: pointer;
    overflow: hidden;
    background: #f5f5f5;
  }

  &__info {
    padding: 12px;
  }
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

.product-name {
  margin: 0 0 4px;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &:hover { color: #ff4d4f; }
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
