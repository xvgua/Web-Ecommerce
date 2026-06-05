<template>
  <div class="dashboard">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据看板</h1>
        <p class="page-subtitle">核心业务指标一览</p>
      </div>
      <div class="dashboard-live">
        <span class="live-dot" />
        实时数据
      </div>
    </div>

    <!-- Flat Stat Cards -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-card__label">总用户数</div>
        <div class="stat-card__body">
          <span class="stat-card__value">{{ stats.totalUsers.toLocaleString() }}</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__label">总订单数</div>
        <div class="stat-card__body">
          <span class="stat-card__value">{{ stats.totalOrders.toLocaleString() }}</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__label">总销售额</div>
        <div class="stat-card__body">
          <span class="stat-card__symbol">&yen;</span>
          <span class="stat-card__value">{{ formatNum(stats.totalSales) }}</span>
          <span class="stat-card__unit">CNY</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__label">今日订单</div>
        <div class="stat-card__body">
          <span class="stat-card__value">{{ stats.todayOrders.toLocaleString() }}</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__label">今日销售额</div>
        <div class="stat-card__body">
          <span class="stat-card__symbol">&yen;</span>
          <span class="stat-card__value">{{ formatNum(stats.todaySales) }}</span>
          <span class="stat-card__unit">CNY</span>
        </div>
      </div>
    </div>

    <!-- Sales Trend -->
    <div class="chart-card" style="margin-top: 28px">
      <div class="chart-card__header">
        <div>
          <h3>销量趋势</h3>
          <p class="chart-card__desc">{{ trendRangeLabel }}</p>
        </div>
        <el-radio-group v-model="trendRange" size="small" @change="loadSalesTrend">
          <el-radio-button value="7d">7 天</el-radio-button>
          <el-radio-button value="30d">30 天</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
      </div>
      <div class="chart-card__body">
        <v-chart :option="salesTrendOption" autoresize style="height:360px" />
      </div>
    </div>

    <!-- Bottom Row -->
    <el-row :gutter="20" style="margin-top: 28px">
      <el-col :span="14">
        <div class="chart-card">
          <div class="chart-card__header">
            <div>
              <h3>热销排行</h3>
              <p class="chart-card__desc">{{ hotRangeLabel }}</p>
            </div>
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
        <div class="chart-card" style="margin-bottom: 20px">
          <div class="chart-card__header">
            <div>
              <h3>品类销量排行</h3>
              <p class="chart-card__desc">各品类累计销量</p>
            </div>
          </div>
          <div class="chart-card__body">
            <v-chart :option="categorySalesOption" autoresize style="height:280px" v-if="categorySales.length" />
            <el-empty v-else description="暂无数据" :image-size="60" />
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card__header">
            <div>
              <h3>订单状态</h3>
              <p class="chart-card__desc">各状态分布</p>
            </div>
          </div>
          <div class="chart-card__body">
            <v-chart :option="orderStatusOption" autoresize style="height:260px" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { Money } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { getDashboardStats, getSalesTrend, getHotProducts, getCategorySales } from '@/api/admin'
import type { SalesTrendItem, HotProductItem, CategorySalesItem } from '@/api/admin'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const trendRange = ref('7d')
const hotRange = ref('all')

const trendRangeLabel = computed(() =>
  ({ '7d': '近 7 日数据', '30d': '近 30 日数据', 'month': '本月累计' })[trendRange.value]
)
const hotRangeLabel = computed(() =>
  ({ 'all': '历史累计', 'week': '近 7 日', 'month': '本月' })[hotRange.value]
)

const stats = reactive<Record<string, number>>({
  totalUsers: 0, totalOrders: 0, totalSales: 0, todayOrders: 0, todaySales: 0,
  pendingOrders: 0, shippingOrders: 0, completedOrders: 0, cancelledOrders: 0,
})

const trendData = ref<SalesTrendItem[]>([])
const hotProducts = ref<HotProductItem[]>([])
const categorySales = ref<CategorySalesItem[]>([])

function formatNum(v: number | string) {
  if (v == null) return '0'
  const n = typeof v === 'string' ? parseFloat(v) : v
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// ── DeepSeek chart palette ──
const CHART_COLORS = ['#3964FE', '#61666B', '#979DA6', '#ADB2B8', '#5686FE', '#81858C']

const salesTrendOption = computed(() => ({
  color: ['#3964FE', '#61666B'],
  tooltip: {
    trigger: 'axis' as const,
    backgroundColor: '#fff',
    borderColor: 'rgba(0,0,0,.06)',
    borderWidth: 1,
    textStyle: { color: '#0F1115', fontSize: 13 },
    boxShadow: '0 4px 12px rgba(15,17,21,.08)',
    axisPointer: { type: 'cross' as const, crossStyle: { color: '#CFD3D6' } },
  },
  legend: {
    data: ['订单量', '销售额'],
    bottom: 0,
    textStyle: { fontSize: 12, color: '#81858C', fontWeight: 500 },
    itemWidth: 12,
    itemHeight: 12,
    itemGap: 24,
  },
  grid: { left: 50, right: 60, top: 20, bottom: 40 },
  xAxis: {
    type: 'category' as const,
    data: trendData.value.map(d => d.date.slice(5)),
    axisLine: { lineStyle: { color: 'rgba(0,0,0,.06)' } },
    axisTick: { show: false },
    axisLabel: { fontSize: 11, color: '#ADB2B8', fontWeight: 500 },
  },
  yAxis: [
    {
      type: 'value' as const,
      name: '单',
      nameTextStyle: { fontSize: 11, color: '#ADB2B8' },
      axisLabel: { fontSize: 11, color: '#ADB2B8' },
      splitLine: { lineStyle: { color: 'rgba(0,0,0,.04)', type: 'dashed' } },
    },
    {
      type: 'value' as const,
      name: '元',
      nameTextStyle: { fontSize: 11, color: '#ADB2B8' },
      axisLabel: { fontSize: 11, color: '#ADB2B8', formatter: (v: number) => `${(v / 1000).toFixed(0)}k` },
      splitLine: { show: false },
    },
  ],
  series: [
    {
      name: '订单量',
      type: 'bar' as const,
      data: trendData.value.map(d => d.orderCount),
      barMaxWidth: 30,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: '#3964FE',
      },
      emphasis: { itemStyle: { color: '#5686FE' } },
    },
    {
      name: '销售额',
      type: 'line' as const,
      yAxisIndex: 1,
      data: trendData.value.map(d => d.salesAmount),
      smooth: true,
      symbolSize: 8,
      lineStyle: { width: 3, color: '#61666B' },
      itemStyle: { color: '#61666B', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear' as const,
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(97,102,107,.10)' },
            { offset: 1, color: 'rgba(97,102,107,0)' },
          ],
        },
      },
    },
  ],
}))

const hotProductsOption = computed(() => ({
  tooltip: {
    trigger: 'axis' as const,
    backgroundColor: '#fff',
    borderColor: 'rgba(0,0,0,.06)',
    borderWidth: 1,
    textStyle: { color: '#0F1115', fontSize: 13 },
    axisPointer: { type: 'shadow' as const },
  },
  grid: { left: 10, right: 50, top: 10, bottom: 30 },
  xAxis: {
    type: 'value' as const,
    axisLabel: { fontSize: 11, color: '#ADB2B8' },
    splitLine: { lineStyle: { color: 'rgba(0,0,0,.04)', type: 'dashed' } },
  },
  yAxis: {
    type: 'category' as const,
    inverse: true,
    data: hotProducts.value.map(p => p.name),
    axisLabel: { fontSize: 11, color: '#61666B', fontWeight: 500 },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [{
    name: '销量',
    type: 'bar' as const,
    data: hotProducts.value.map((p, i) => ({
      value: p.sales,
      itemStyle: {
        color: CHART_COLORS[i % CHART_COLORS.length],
        borderRadius: [0, 6, 6, 0],
      },
    })),
    barMaxWidth: 18,
    label: { show: true, position: 'right' as const, fontSize: 11, color: '#61666B', fontWeight: 600 },
  }],
}))

const categorySalesOption = computed(() => ({
  color: ['#3964FE', '#61666B', '#979DA6', '#ADB2B8', '#5686FE', '#81858C', '#7B8EBD', '#5B6C93'],
  tooltip: {
    trigger: 'item' as const,
    backgroundColor: '#fff',
    borderColor: 'rgba(0,0,0,.06)',
    borderWidth: 1,
    textStyle: { color: '#0F1115', fontSize: 13 },
    formatter: '{b}: {c} 件 ({d}%)',
  },
  legend: {
    orient: 'vertical' as const,
    right: 6,
    top: 'center',
    textStyle: { fontSize: 11, color: '#61666B', fontWeight: 500 },
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 10,
  },
  series: [{
    type: 'pie' as const,
    radius: ['50%', '80%'],
    center: ['42%', '50%'],
    avoidLabelOverlap: false,
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
      scaleSize: 6,
    },
    itemStyle: {
      borderColor: '#fff',
      borderWidth: 3,
      borderRadius: 4,
    },
    data: categorySales.value.map(c => ({ name: c.categoryName, value: c.sales })),
  }],
}))

const orderStatusOption = computed(() => ({
  color: ['#3964FE', '#61666B', '#979DA6', '#ADB2B8'],
  tooltip: {
    trigger: 'item' as const,
    backgroundColor: '#fff',
    borderColor: 'rgba(0,0,0,.06)',
    borderWidth: 1,
    textStyle: { color: '#0F1115', fontSize: 13 },
    formatter: '{b}: {c} 单 ({d}%)',
  },
  legend: {
    orient: 'vertical' as const,
    right: 10,
    top: 'center',
    textStyle: { fontSize: 12, color: '#61666B', fontWeight: 500 },
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 16,
  },
  series: [{
    type: 'pie' as const,
    radius: ['55%', '80%'],
    center: ['38%', '50%'],
    avoidLabelOverlap: false,
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 16, fontWeight: 'bold' },
      scaleSize: 8,
    },
    itemStyle: {
      borderColor: '#fff',
      borderWidth: 4,
      borderRadius: 6,
    },
    data: [
      { value: stats.pendingOrders, name: '待支付' },
      { value: stats.shippingOrders, name: '待发货/已发货' },
      { value: stats.completedOrders, name: '已完成' },
      { value: stats.cancelledOrders, name: '已取消' },
    ],
  }],
}))

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

async function loadCategorySales() {
  const res = await getCategorySales()
  categorySales.value = res.data || []
}

async function loadAll() {
  await Promise.all([loadStats(), loadSalesTrend(), loadHotProducts(), loadCategorySales()])
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
.dashboard {
  // max-width handled by .content-wrapper
}

/* ── Live indicator ── */
.dashboard-live {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-s);
  color: var(--text-muted);
  font-weight: var(--font-weight-strong);
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22C55E;
  box-shadow: 0 0 8px rgba(34, 197, 94, .4);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: .35; transform: scale(.75); }
}

/* ═══════════════════════════════════════
   Flat Stat Cards — DeepSeek style
   ═══════════════════════════════════════ */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 32px 12px;

  @media (max-width: 1300px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 900px)  { grid-template-columns: repeat(2, 1fr); }
}

.stat-card {
  display: flex;
  flex-direction: column;

  &__label {
    font-size: var(--font-size-s);
    color: var(--text-secondary);
    margin-bottom: 10px;
    line-height: var(--font-size-s);
  }

  &__body {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  &__symbol {
    font-size: 18px;
    color: var(--text-secondary);
    line-height: 18px;
  }

  &__value {
    font-size: 28px;
    font-weight: var(--font-weight-strong);
    color: var(--text-primary);
    line-height: 28px;
    font-variant-numeric: tabular-nums;
  }

  &__unit {
    font-size: 18px;
    color: var(--text-secondary);
    line-height: 18px;
    margin-left: 6px;
  }
}

/* ═══════════════════════════════════════
   Chart Cards
   ═══════════════════════════════════════ */
.chart-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-l1);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow var(--duration) var(--ease-in-out);

  &:hover {
    box-shadow: var(--shadow-sm);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 20px 24px 0;

    h3 {
      font-size: var(--font-size-l);
      font-weight: var(--font-weight-strong);
      color: var(--text-primary);
      letter-spacing: 0;
    }
  }

  &__desc {
    font-size: var(--font-size-s);
    color: var(--text-muted);
    margin-top: 2px;
    font-weight: var(--font-weight-normal);
  }

  &__body {
    padding: 12px 24px 20px;
  }
}
</style>
