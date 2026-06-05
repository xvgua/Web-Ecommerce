<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1 class="page-title">数据看板</h1>
    </div>

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-card stat-card--blue">
        <div class="stat-card__icon"><el-icon size="28"><User /></el-icon></div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.totalUsers.toLocaleString() }}</div>
          <div class="stat-card__label">总用户数</div>
        </div>
      </div>
      <div class="stat-card stat-card--green">
        <div class="stat-card__icon"><el-icon size="28"><Document /></el-icon></div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.totalOrders.toLocaleString() }}</div>
          <div class="stat-card__label">总订单数</div>
        </div>
      </div>
      <div class="stat-card stat-card--orange">
        <div class="stat-card__icon"><el-icon size="28"><Money /></el-icon></div>
        <div class="stat-card__body">
          <div class="stat-card__value">&yen;{{ formatNum(stats.totalSales) }}</div>
          <div class="stat-card__label">总销售额</div>
        </div>
      </div>
      <div class="stat-card stat-card--purple">
        <div class="stat-card__icon"><el-icon size="28"><TrendCharts /></el-icon></div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.todayOrders }}</div>
          <div class="stat-card__label">今日订单</div>
        </div>
      </div>
      <div class="stat-card stat-card--teal">
        <div class="stat-card__icon"><el-icon size="28"><Wallet /></el-icon></div>
        <div class="stat-card__body">
          <div class="stat-card__value">&yen;{{ formatNum(stats.todaySales) }}</div>
          <div class="stat-card__label">今日销售额</div>
        </div>
      </div>
    </div>

    <!-- Sales Trend Chart -->
    <div class="chart-card" style="margin-top: 24px">
      <div class="chart-card__header">
        <h3>销量趋势</h3>
        <el-radio-group v-model="trendRange" size="small" @change="loadSalesTrend">
          <el-radio-button value="7d">7天</el-radio-button>
          <el-radio-button value="30d">30天</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
      </div>
      <div class="chart-card__body">
        <v-chart :option="salesTrendOption" autoresize style="height:320px" />
      </div>
    </div>

    <!-- Bottom Row: Hot Products + Order Status -->
    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="14">
        <div class="chart-card">
          <div class="chart-card__header">
            <h3>热销商品排行</h3>
            <el-radio-group v-model="hotRange" size="small" @change="loadHotProducts">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
            </el-radio-group>
          </div>
          <div class="chart-card__body">
            <v-chart :option="hotProductsOption" autoresize style="height:320px" />
          </div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-card">
          <div class="chart-card__header">
            <h3>订单状态</h3>
          </div>
          <div class="chart-card__body">
            <v-chart :option="orderStatusOption" autoresize style="height:320px" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { User, Document, Money, TrendCharts, Wallet } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { getDashboardStats, getSalesTrend, getHotProducts } from '@/api/admin'
import type { SalesTrendItem, HotProductItem } from '@/api/admin'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const trendRange = ref('7d')
const hotRange = ref('all')

const stats = reactive<Record<string, number>>({
  totalUsers: 0, totalOrders: 0, totalSales: 0, todayOrders: 0, todaySales: 0,
  pendingOrders: 0, shippingOrders: 0, completedOrders: 0, cancelledOrders: 0,
})

const trendData = ref<SalesTrendItem[]>([])
const hotProducts = ref<HotProductItem[]>([])

function formatNum(v: number | string) {
  if (v == null) return '0'
  const n = typeof v === 'string' ? parseFloat(v) : v
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// ── ECharts Options ──

const salesTrendOption = computed(() => ({
  tooltip: {
    trigger: 'axis' as const,
    axisPointer: { type: 'cross' as const },
  },
  legend: { data: ['订单量', '销售额'], bottom: 0 },
  grid: { left: 60, right: 60, top: 20, bottom: 40 },
  xAxis: {
    type: 'category' as const,
    data: trendData.value.map(d => d.date.slice(5)),
    axisLabel: { fontSize: 11 },
  },
  yAxis: [
    {
      type: 'value' as const,
      name: '订单量',
      axisLabel: { fontSize: 11 },
    },
    {
      type: 'value' as const,
      name: '销售额(元)',
      axisLabel: { fontSize: 11, formatter: (v: number) => `${(v / 1000).toFixed(0)}k` },
    },
  ],
  series: [
    {
      name: '订单量',
      type: 'bar' as const,
      data: trendData.value.map(d => d.orderCount),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 30,
    },
    {
      name: '销售额',
      type: 'line' as const,
      yAxisIndex: 1,
      data: trendData.value.map(d => d.salesAmount),
      smooth: true,
      itemStyle: { color: '#e6a23c' },
      lineStyle: { width: 2 },
      symbolSize: 6,
    },
  ],
}))

const hotProductsOption = computed(() => ({
  tooltip: {
    trigger: 'axis' as const,
    axisPointer: { type: 'shadow' as const },
  },
  legend: { data: ['销量', '销售额'], bottom: 0 },
  grid: { left: 10, right: 60, top: 10, bottom: 40 },
  xAxis: {
    type: 'value' as const,
    axisLabel: { fontSize: 11 },
  },
  yAxis: {
    type: 'category' as const,
    inverse: true,
    data: hotProducts.value.map(p => p.name.length > 10 ? p.name.slice(0, 10) + '...' : p.name),
    axisLabel: { fontSize: 11 },
  },
  series: [
    {
      name: '销量',
      type: 'bar' as const,
      data: hotProducts.value.map(p => p.sales),
      itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
      barMaxWidth: 18,
      label: { show: true, position: 'right' as const, fontSize: 11 },
    },
  ],
}))

const orderStatusOption = computed(() => ({
  tooltip: { trigger: 'item' as const, formatter: '{b}: {c} 单 ({d}%)' },
  legend: { orient: 'vertical' as const, right: 10, top: 'center', itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie' as const,
    radius: ['50%', '75%'],
    center: ['38%', '50%'],
    avoidLabelOverlap: false,
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
    },
    data: [
      { value: stats.pendingOrders, name: '待支付', itemStyle: { color: '#409eff' } },
      { value: stats.shippingOrders, name: '待发货/已发货', itemStyle: { color: '#e6a23c' } },
      { value: stats.completedOrders, name: '已完成', itemStyle: { color: '#67c23a' } },
      { value: stats.cancelledOrders, name: '已取消', itemStyle: { color: '#f56c6c' } },
    ],
  }],
}))

// ── Data Loading ──

async function loadStats() {
  const res = await getDashboardStats()
  Object.assign(stats, res.data)
}

async function loadSalesTrend() {
  const res = await getSalesTrend(trendRange.value)
  trendData.value = res.data
}

async function loadHotProducts() {
  const res = await getHotProducts(hotRange.value)
  hotProducts.value = res.data
}

async function loadAll() {
  await Promise.all([loadStats(), loadSalesTrend(), loadHotProducts()])
}

let refreshTimer: number | null = null

onMounted(() => {
  loadAll()
  refreshTimer = window.setInterval(loadAll, 30 * 60 * 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style lang="scss" scoped>
.dashboard { max-width: 1400px; }

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 0;
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
</style>
