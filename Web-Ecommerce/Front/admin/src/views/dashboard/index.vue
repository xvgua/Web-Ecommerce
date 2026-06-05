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

    <!-- Biomorphic Stat Cards -->
    <div class="stat-garden">
      <div class="bio-card bio-card--sage">
        <div class="bio-card__blob" />
        <div class="bio-card__icon-wrap">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="bio-card__body">
          <div class="bio-card__value">{{ stats.totalUsers.toLocaleString() }}</div>
          <div class="bio-card__label">总用户数</div>
        </div>
      </div>

      <div class="bio-card bio-card--amber">
        <div class="bio-card__blob" />
        <div class="bio-card__icon-wrap">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="bio-card__body">
          <div class="bio-card__value">{{ stats.totalOrders.toLocaleString() }}</div>
          <div class="bio-card__label">总订单数</div>
        </div>
      </div>

      <div class="bio-card bio-card--coral">
        <div class="bio-card__blob" />
        <div class="bio-card__icon-wrap">
          <el-icon :size="24"><Money /></el-icon>
        </div>
        <div class="bio-card__body">
          <div class="bio-card__value">&yen;{{ formatNum(stats.totalSales) }}</div>
          <div class="bio-card__label">总销售额</div>
        </div>
      </div>

      <div class="bio-card bio-card--lavender">
        <div class="bio-card__blob" />
        <div class="bio-card__icon-wrap">
          <el-icon :size="24"><TrendCharts /></el-icon>
        </div>
        <div class="bio-card__body">
          <div class="bio-card__value">{{ stats.todayOrders }}</div>
          <div class="bio-card__label">今日订单</div>
        </div>
      </div>

      <div class="bio-card bio-card--sky">
        <div class="bio-card__blob" />
        <div class="bio-card__icon-wrap">
          <el-icon :size="24"><Wallet /></el-icon>
        </div>
        <div class="bio-card__body">
          <div class="bio-card__value">&yen;{{ formatNum(stats.todaySales) }}</div>
          <div class="bio-card__label">今日销售额</div>
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
        <div class="chart-card">
          <div class="chart-card__header">
            <div>
              <h3>订单状态</h3>
              <p class="chart-card__desc">各状态分布</p>
            </div>
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

function formatNum(v: number | string) {
  if (v == null) return '0'
  const n = typeof v === 'string' ? parseFloat(v) : v
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// ── Organic chart colors ──
const ORG_COLORS = ['#6eb89a', '#e8a860', '#e08880', '#b8b0e0', '#8ab8d8', '#a8d8c0']

const salesTrendOption = computed(() => ({
  color: ['#6eb89a', '#e8a860'],
  tooltip: {
    trigger: 'axis' as const,
    backgroundColor: '#fff',
    borderColor: '#e8e4dc',
    borderWidth: 1,
    textStyle: { color: '#2d2a26', fontSize: 13 },
    boxShadow: '0 8px 32px rgba(44,40,32,.08)',
    axisPointer: { type: 'cross' as const, crossStyle: { color: '#c4bbb0' } },
  },
  legend: {
    data: ['订单量', '销售额'],
    bottom: 0,
    textStyle: { fontSize: 12, color: '#7a7570', fontWeight: 500 },
    itemWidth: 12,
    itemHeight: 12,
    itemGap: 24,
  },
  grid: { left: 50, right: 60, top: 20, bottom: 40 },
  xAxis: {
    type: 'category' as const,
    data: trendData.value.map(d => d.date.slice(5)),
    axisLine: { lineStyle: { color: '#e8e4dc' } },
    axisTick: { show: false },
    axisLabel: { fontSize: 11, color: '#a8a49e', fontWeight: 500 },
  },
  yAxis: [
    {
      type: 'value' as const,
      name: '单',
      nameTextStyle: { fontSize: 11, color: '#a8a49e' },
      axisLabel: { fontSize: 11, color: '#a8a49e' },
      splitLine: { lineStyle: { color: '#f0ece6', type: 'dashed' } },
    },
    {
      type: 'value' as const,
      name: '元',
      nameTextStyle: { fontSize: 11, color: '#a8a49e' },
      axisLabel: { fontSize: 11, color: '#a8a49e', formatter: (v: number) => `${(v / 1000).toFixed(0)}k` },
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
        borderRadius: [8, 8, 0, 0],
        color: '#6eb89a',
      },
      emphasis: { itemStyle: { color: '#8cc9aa' } },
    },
    {
      name: '销售额',
      type: 'line' as const,
      yAxisIndex: 1,
      data: trendData.value.map(d => d.salesAmount),
      smooth: true,
      symbolSize: 8,
      lineStyle: { width: 3, color: '#e8a860' },
      itemStyle: { color: '#e8a860', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear' as const,
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(232,168,96,.15)' },
            { offset: 1, color: 'rgba(232,168,96,0)' },
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
    borderColor: '#e8e4dc',
    borderWidth: 1,
    textStyle: { color: '#2d2a26', fontSize: 13 },
    axisPointer: { type: 'shadow' as const },
  },
  grid: { left: 10, right: 50, top: 10, bottom: 30 },
  xAxis: {
    type: 'value' as const,
    axisLabel: { fontSize: 11, color: '#a8a49e' },
    splitLine: { lineStyle: { color: '#f0ece6', type: 'dashed' } },
  },
  yAxis: {
    type: 'category' as const,
    inverse: true,
    data: hotProducts.value.map(p => p.name.length > 12 ? p.name.slice(0, 12) + '...' : p.name),
    axisLabel: { fontSize: 11, color: '#7a7570', fontWeight: 500 },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [{
    name: '销量',
    type: 'bar' as const,
    data: hotProducts.value.map((p, i) => ({
      value: p.sales,
      itemStyle: {
        color: ORG_COLORS[i % ORG_COLORS.length],
        borderRadius: [0, 8, 8, 0],
      },
    })),
    barMaxWidth: 18,
    label: { show: true, position: 'right' as const, fontSize: 11, color: '#7a7570', fontWeight: 600 },
  }],
}))

const orderStatusOption = computed(() => ({
  color: ['#6eb89a', '#e8a860', '#8ab8d8', '#e08880'],
  tooltip: {
    trigger: 'item' as const,
    backgroundColor: '#fff',
    borderColor: '#e8e4dc',
    borderWidth: 1,
    textStyle: { color: '#2d2a26', fontSize: 13 },
    formatter: '{b}: {c} 单 ({d}%)',
  },
  legend: {
    orient: 'vertical' as const,
    right: 10,
    top: 'center',
    textStyle: { fontSize: 12, color: '#7a7570', fontWeight: 500 },
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
.dashboard {
  max-width: 1440px;
}

/* ── Live indicator ── */
.dashboard-live {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--org-text-muted);
  font-weight: 600;
}

.live-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #6eb89a;
  box-shadow: 0 0 10px rgba(110, 184, 154, .5);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: .35; transform: scale(.75); }
}

/* ═══════════════════════════════════════
   Biomorphic Stat Cards
   ═══════════════════════════════════════ */
.stat-garden {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;

  @media (max-width: 1300px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 900px)  { grid-template-columns: repeat(2, 1fr); }
}

.bio-card {
  background: var(--org-surface);
  border: 1px solid var(--org-border);
  border-radius: var(--org-radius-xl);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  position: relative;
  overflow: hidden;
  cursor: default;
  transition: all var(--org-duration) var(--org-ease);

  &:hover {
    transform: translateY(-3px);
    border-color: transparent;
  }

  // Organic blob decoration
  &__blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(36px);
    opacity: .12;
    pointer-events: none;
    transition: all var(--org-duration) var(--org-ease);
  }

  &--sage {
    .bio-card__blob { width: 100px; height: 100px; background: #6eb89a; top: -30px; right: -20px; }
    &:hover { box-shadow: 0 8px 32px rgba(110, 184, 154, .15); }
  }
  &--amber {
    .bio-card__blob { width: 90px; height: 90px; background: #e8a860; top: -25px; right: -15px; }
    &:hover { box-shadow: 0 8px 32px rgba(232, 168, 96, .15); }
  }
  &--coral {
    .bio-card__blob { width: 95px; height: 95px; background: #e08880; top: -28px; right: -18px; }
    &:hover { box-shadow: 0 8px 32px rgba(224, 136, 128, .15); }
  }
  &--lavender {
    .bio-card__blob { width: 88px; height: 88px; background: #b8b0e0; top: -22px; right: -12px; }
    &:hover { box-shadow: 0 8px 32px rgba(184, 176, 224, .15); }
  }
  &--sky {
    .bio-card__blob { width: 92px; height: 92px; background: #8ab8d8; top: -26px; right: -16px; }
    &:hover { box-shadow: 0 8px 32px rgba(138, 184, 216, .15); }
  }

  &:hover &__blob {
    opacity: .2;
    transform: scale(1.15);
  }

  &__icon-wrap {
    width: 52px;
    height: 52px;
    border-radius: var(--org-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    position: relative;
    z-index: 1;
  }

  &--sage    &__icon-wrap { background: #edf7f3; color: #6eb89a; }
  &--amber   &__icon-wrap { background: #fdf6ed; color: #e8a860; }
  &--coral   &__icon-wrap { background: #fdf0ef; color: #e08880; }
  &--lavender &__icon-wrap { background: #f5f3fc; color: #b8b0e0; }
  &--sky     &__icon-wrap { background: #eef6fb; color: #8ab8d8; }

  &__body {
    flex: 1;
    min-width: 0;
    position: relative;
    z-index: 1;
  }

  &__value {
    font-size: 27px;
    font-weight: 700;
    color: var(--org-text);
    letter-spacing: -.6px;
    line-height: 1.2;
  }

  &__label {
    font-size: 13px;
    color: var(--org-text-muted);
    margin-top: 4px;
    font-weight: 600;
  }
}

/* ═══════════════════════════════════════
   Chart Cards
   ═══════════════════════════════════════ */
.chart-card {
  background: var(--org-surface);
  border: 1px solid var(--org-border);
  border-radius: var(--org-radius-xl);
  overflow: hidden;
  transition: all var(--org-duration) var(--org-ease-soft);

  &:hover {
    box-shadow: var(--org-shadow-md);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 24px 28px 0;

    h3 {
      font-size: 17px;
      font-weight: 700;
      color: var(--org-text);
      letter-spacing: -.3px;
    }
  }

  &__desc {
    font-size: 12.5px;
    color: var(--org-text-muted);
    margin-top: 3px;
    font-weight: 500;
  }

  &__body {
    padding: 16px 28px 24px;
  }
}
</style>
