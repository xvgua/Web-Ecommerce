<template>
  <div class="user-sidebar">
    <!-- Logged In: User Info -->
    <div v-if="userStore.isLoggedIn" class="user-sidebar__info">
      <router-link to="/user/profile" class="user-sidebar__avatar-link">
        <el-avatar :size="56" :src="userStore.user?.avatar" class="user-sidebar__avatar">
          <span>{{ userStore.user?.username?.[0]?.toUpperCase() }}</span>
        </el-avatar>
      </router-link>
      <div class="user-sidebar__meta">
        <span class="user-sidebar__nick">{{ userStore.user?.username }}</span>
        <div class="user-sidebar__btns">
          <router-link to="/user/profile">个人中心</router-link>
          <router-link to="/orders">我的订单</router-link>
        </div>
      </div>
    </div>

    <!-- Logged Out: Login Prompt -->
    <div v-else class="user-sidebar__login-guide">
      <div class="user-sidebar__login-avatar">
        <el-icon :size="40"><UserFilled /></el-icon>
      </div>
      <p class="user-sidebar__login-title">登录后更精彩</p>
      <p class="user-sidebar__login-desc">更懂你的推荐，更便捷的搜索</p>
      <router-link to="/login" class="user-sidebar__login-btn">立即登录</router-link>
    </div>

    <!-- Order Stats -->
    <div v-if="userStore.isLoggedIn" class="user-sidebar__stats">
      <router-link to="/orders?status=0" class="user-sidebar__stat">
        <strong>{{ orderStats.pendingPayment }}</strong>
        <span>待付款</span>
      </router-link>
      <router-link to="/orders?status=1" class="user-sidebar__stat">
        <strong>{{ orderStats.pendingShipment }}</strong>
        <span>待发货</span>
      </router-link>
      <router-link to="/orders?status=2" class="user-sidebar__stat">
        <strong>{{ orderStats.pendingReceipt }}</strong>
        <span>待收货</span>
      </router-link>
      <router-link to="/orders?reviewFilter=pending" class="user-sidebar__stat">
        <strong>{{ orderStats.pendingReview }}</strong>
        <span>待评价</span>
      </router-link>
      <router-link to="/cart" class="user-sidebar__stat">
        <strong>{{ cartStore.totalCount }}</strong>
        <span>购物车</span>
      </router-link>
    </div>

    <!-- 2×2 Entry Grid -->
    <div class="user-sidebar__entries">
      <router-link to="/cart" class="user-sidebar__entry">
        <span class="user-sidebar__entry-icon">🛒</span>
        <span class="user-sidebar__entry-value">{{ cartStore.totalCount }}</span>
        <span class="user-sidebar__entry-label">购物车</span>
      </router-link>
      <router-link to="/user/favorites" class="user-sidebar__entry">
        <span class="user-sidebar__entry-icon">⭐</span>
        <span class="user-sidebar__entry-value">{{ favoriteCount }}</span>
        <span class="user-sidebar__entry-label">我的收藏</span>
      </router-link>
      <router-link to="/coupons" class="user-sidebar__entry">
        <span class="user-sidebar__entry-icon">🎫</span>
        <span class="user-sidebar__entry-value">{{ couponCount }}张</span>
        <span class="user-sidebar__entry-label">优惠券</span>
      </router-link>
      <div class="user-sidebar__entry user-sidebar__entry--saved">
        <span class="user-sidebar__entry-icon">💡</span>
        <span class="user-sidebar__entry-value">{{ formatPrice(savedAmount) }}</span>
        <span class="user-sidebar__entry-label">已节省</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { UserFilled, Bell, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useCouponStore } from '@/stores/coupon'
import request from '@/api/request'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const couponStore = useCouponStore()

const couponCount = ref(0)
const savedAmount = ref(0)
const favoriteCount = ref(0)
const latestAnnouncement = ref<{ title: string } | null>(null)

const orderStats = reactive({
  pendingPayment: 0,
  pendingShipment: 0,
  pendingReceipt: 0,
  pendingReview: 0,
})

async function loadSidebarData() {
  const skipOpt = { _skipErrorToast: true } as Record<string, unknown>

  if (userStore.isLoggedIn) {
    try {
      const res = await request.get('/orders/stats', skipOpt as any)
      Object.assign(orderStats, res.data)
    } catch { /* silent */ }

    try {
      await cartStore.fetchCart(true)
    } catch { /* silent */ }

    try {
      const res = await request.get('/user/coupons', { params: { status: 0, page: 1, pageSize: 1 }, ...skipOpt } as any)
      couponCount.value = res.data?.total || 0
    } catch { /* silent */ }

    try {
      const usedRes = await request.get('/user/coupons', { params: { status: 1, page: 1, pageSize: 100 }, ...skipOpt } as any)
      const records = usedRes.data?.records || []
      savedAmount.value = records.reduce(
        (sum: number, uc: any) => sum + (uc.coupon?.discount || 0),
        0,
      )
    } catch { /* silent */ }

    try {
      const favRes = await request.get('/favorites', skipOpt as any)
      favoriteCount.value = (favRes.data || []).length
    } catch { /* silent */ }
  }

  try {
    const res = await request.get('/announcements', { params: { limit: 1 }, ...skipOpt } as any)
    if ((res.data || []).length) {
      latestAnnouncement.value = res.data[0]
    }
  } catch { /* silent */ }
}

onMounted(() => { loadSidebarData() })

// Refresh sidebar data when returning from key flows (order, payment, review, etc.)
watch(() => route.path, (path) => {
  if (!userStore.isLoggedIn) return
  if (path === '/' || path.startsWith('/orders') || path.startsWith('/user')) {
    loadSidebarData()
  }
})

// Refresh coupon count when a coupon is claimed elsewhere
watch(() => couponStore.version, () => {
  if (!userStore.isLoggedIn) return
  const skipOpt = { _skipErrorToast: true } as Record<string, unknown>
  request.get('/user/coupons', { params: { status: 0, page: 1, pageSize: 1 }, ...skipOpt } as any)
    .then(res => { couponCount.value = res.data?.total || 0 })
    .catch(() => {})
})
</script>

<style lang="scss" scoped>
.user-sidebar {
  background: var(--bg1);
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--line-light);
  display: flex;
  flex-direction: column;
  min-height: 100%;

  > :last-child {
    margin-top: auto;
  }
}

/* ── User Info ── */
.user-sidebar__info {
  background: transparent;
  padding: 20px 16px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  border-bottom: 1px solid var(--line-light);
}

.user-sidebar__avatar-link {
  flex-shrink: 0;
}

.user-sidebar__avatar {
  border: 2px solid #fff;
  box-shadow: var(--shadow-md);
  cursor: pointer;
  transition: transform var(--transition-fast);

  &:hover { transform: scale(1.05); }
}

.user-sidebar__meta {
  min-width: 0;
}

.user-sidebar__nick {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--text1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}

.user-sidebar__btns {
  display: flex;
  gap: 8px;

  a {
    padding: 5px 14px;
    border-radius: 16px;
    font-size: 11px;
    font-weight: 500;
    background: rgba(255, 255, 255, 0.06);
    color: var(--text2);
    border: 1px solid var(--line-light);
    transition: all var(--transition-fast);

    &:hover {
      color: var(--brand-primary);
      border-color: var(--brand-primary);
      background: rgba(139, 92, 246, 0.10);
      text-decoration: none;
    }
  }
}

/* ── Login Guide ── */
.user-sidebar__login-guide {
  background: transparent;
  padding: 24px 20px 20px;
  text-align: center;
  border-bottom: 1px solid var(--line-light);
}

.user-sidebar__login-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--bg2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  color: var(--text3);
}

.user-sidebar__login-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text1);
  margin: 0 0 4px;
}

.user-sidebar__login-desc {
  font-size: 12px;
  color: var(--text3);
  margin: 0 0 16px;
}

.user-sidebar__login-btn {
  display: inline-block;
  padding: 8px 32px;
  border-radius: var(--radius-full);
  background: var(--brand-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  transition: background var(--transition-fast);

  &:hover {
    background: var(--brand-primary-hover);
    text-decoration: none;
    color: #fff;
  }
}

/* ── Order Stats ── */
.user-sidebar__stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  padding: 12px 4px;
  background: transparent;
  border-bottom: 1px solid var(--line-light);
}

.user-sidebar__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--text3);
  transition: all var(--transition-fast);
  padding: 4px 2px;

  strong {
    font-size: 18px;
    font-weight: 700;
    color: var(--text1);
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
  }

  &:hover {
    color: var(--brand-primary);
    text-decoration: none;

    strong { color: var(--brand-primary); }
  }
}

/* ── Benefit Entries ── */
.user-sidebar__entries {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--line-light);

}

.user-sidebar__entry {
  background: var(--bg1);
  padding: 14px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 78px;
  min-width: 0;
  font-size: 11px;
  color: var(--text3);
  transition: all var(--transition-fast);
  cursor: pointer;
  overflow: hidden;

  &-icon { font-size: 18px; }
  &-value {
    font-size: 17px;
    font-weight: 700;
    color: var(--text1);
    font-variant-numeric: tabular-nums;
    max-width: 100%;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  &-label { font-size: 11px; color: var(--text3); }

  &:hover {
    color: var(--brand-primary);
    text-decoration: none;

    .user-sidebar__entry-value { color: var(--brand-primary); }
  }

  &--saved {
    cursor: default;

    &:hover {
      color: var(--text3);
      .user-sidebar__entry-value { color: var(--text1); }
    }
  }
}

</style>
