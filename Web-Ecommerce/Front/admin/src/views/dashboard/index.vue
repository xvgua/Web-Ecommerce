<template>
  <div class="dashboard">
    <h1 class="page-title">数据看板</h1>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总用户数" :value="stats.totalUsers" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总订单数" :value="stats.totalOrders" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总销售额" :value="stats.totalSales" prefix="¥" :precision="2" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日订单" :value="stats.todayOrders" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日销售额" :value="stats.todaySales" prefix="¥" :precision="2" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDashboardStats } from '@/api/admin'

const stats = reactive({
  totalUsers: 0,
  totalOrders: 0,
  totalSales: 0,
  todayOrders: 0,
  todaySales: 0,
})

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    Object.assign(stats, res.data)
  } catch {
    // handled by interceptor
  }
})
</script>

<style lang="scss" scoped>
.dashboard {
  .stats-row {
    .el-card {
      :deep(.el-statistic__head) {
        font-size: 14px;
        color: #999;
      }
      :deep(.el-statistic__number) {
        font-size: 28px;
      }
    }
  }
}
</style>
