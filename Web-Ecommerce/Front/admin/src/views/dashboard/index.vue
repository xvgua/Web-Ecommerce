<template>
  <div class="dashboard">
    <h1 class="page-title">数据看板</h1>

    <div class="stat-cards">
      <div class="stat-card stat-card--blue">
        <div class="stat-card__icon">
          <el-icon size="28"><User /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.totalUsers.toLocaleString() }}</div>
          <div class="stat-card__label">总用户数</div>
        </div>
      </div>
      <div class="stat-card stat-card--green">
        <div class="stat-card__icon">
          <el-icon size="28"><Document /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.totalOrders.toLocaleString() }}</div>
          <div class="stat-card__label">总订单数</div>
        </div>
      </div>
      <div class="stat-card stat-card--orange">
        <div class="stat-card__icon">
          <el-icon size="28"><Money /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__value">&yen;{{ stats.totalSales.toLocaleString() }}</div>
          <div class="stat-card__label">总销售额</div>
        </div>
      </div>
      <div class="stat-card stat-card--purple">
        <div class="stat-card__icon">
          <el-icon size="28"><TrendCharts /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.todayOrders }}</div>
          <div class="stat-card__label">今日订单</div>
        </div>
      </div>
      <div class="stat-card stat-card--teal">
        <div class="stat-card__icon">
          <el-icon size="28"><Wallet /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__value">&yen;{{ stats.todaySales.toLocaleString() }}</div>
          <div class="stat-card__label">今日销售额</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="14">
        <div class="chart-card">
          <div class="chart-card__header">
            <h3>热销商品排行</h3>
            <el-tag size="small">本周</el-tag>
          </div>
          <div class="chart-card__body chart-placeholder">
            <svg viewBox="0 0 500 180" xmlns="http://www.w3.org/2000/svg">
              <polyline points="10,140 60,100 110,120 160,60 210,80 260,30 310,50 360,20 410,35 460,10"
                fill="none" stroke="#409eff" stroke-width="3" stroke-linecap="round" />
              <polygon points="10,140 60,100 110,120 160,60 210,80 260,30 310,50 360,20 410,35 460,10 460,180 10,180"
                fill="rgba(64,158,255,.08)" />
            </svg>
            <p style="text-align:center;color:#999;margin-top:8px;font-size:13px">连接后端后将展示真实销售趋势图表</p>
          </div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-card">
          <div class="chart-card__header">
            <h3>订单概览</h3>
          </div>
          <div class="chart-card__body">
            <div class="order-stats">
              <div class="order-stat-item">
                <span class="order-stat-dot" style="background:#409eff" />
                <span>待支付</span>
                <strong>{{ stats.pendingOrders || 0 }}</strong>
              </div>
              <div class="order-stat-item">
                <span class="order-stat-dot" style="background:#e6a23c" />
                <span>待发货</span>
                <strong>{{ stats.shippingOrders || 0 }}</strong>
              </div>
              <div class="order-stat-item">
                <span class="order-stat-dot" style="background:#67c23a" />
                <span>已完成</span>
                <strong>{{ stats.completedOrders || 0 }}</strong>
              </div>
              <div class="order-stat-item">
                <span class="order-stat-dot" style="background:#f56c6c" />
                <span>已取消</span>
                <strong>{{ stats.cancelledOrders || 0 }}</strong>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import {
  User, Document, Money, TrendCharts, Wallet,
} from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/admin'

const stats = reactive<Record<string, number>>({
  totalUsers: 0,
  totalOrders: 0,
  totalSales: 0,
  todayOrders: 0,
  todaySales: 0,
  pendingOrders: 0,
  shippingOrders: 0,
  completedOrders: 0,
  cancelledOrders: 0,
})

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    Object.assign(stats, res.data)
  } catch { /* handled by interceptor */ }
})
</script>

<style lang="scss" scoped>
.dashboard { max-width: 1400px; }

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
}

/* ── Stat Cards ── */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;

  @media (max-width: 1200px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 768px)  { grid-template-columns: repeat(2, 1fr); }
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
  transition: transform .15s, box-shadow .15s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0,0,0,.08);
  }

  &__icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
  }

  &--blue  &__icon { background: linear-gradient(135deg, #667eea, #764ba2); }
  &--green &__icon { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #333; }
  &--orange &__icon { background: linear-gradient(135deg, #fa709a, #fee140); color: #333; }
  &--purple &__icon { background: linear-gradient(135deg, #a18cd1, #fbc2eb); }
  &--teal  &__icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }

  &__body { flex: 1; }

  &__value {
    font-size: 24px;
    font-weight: 700;
    color: #1a1a1a;
    font-family: 'SF Mono', monospace;
  }

  &__label {
    font-size: 13px;
    color: #999;
    margin-top: 4px;
  }
}

/* ── Chart Cards ── */
.chart-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px 0;

    h3 { font-size: 16px; font-weight: 600; }
  }

  &__body {
    padding: 16px 20px;
  }
}

.chart-placeholder svg {
  width: 100%;
  height: 160px;
}

.order-stats {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 8px;
}

.order-stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #666;

  strong { margin-left: auto; font-size: 18px; color: #333; }
}

.order-stat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
