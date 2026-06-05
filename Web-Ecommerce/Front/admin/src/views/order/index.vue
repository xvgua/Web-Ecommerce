<template>
  <div class="order-manage">
    <h1 class="page-title">订单管理</h1>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索订单号" clearable class="toolbar-search" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input v-model="userId" placeholder="用户ID" clearable class="toolbar-select" @keyup.enter="handleSearch">
        <template #prefix><el-icon><User /></el-icon></template>
      </el-input>
      <el-select v-model="status" placeholder="订单状态" clearable class="toolbar-select" @change="handleSearch">
        <el-option v-for="(text, val) in ORDER_STATUS_MAP" :key="val" :label="text" :value="Number(val)" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="toolbar-actions">
      <el-button :loading="exporting" @click="handleExport">
        <el-icon><Download /></el-icon> 导出Excel
      </el-button>
    </div>

    <el-dialog v-model="exportErrorVisible" title="导出失败" width="440px">
      <p>{{ exportErrorMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="exportErrorVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <div class="table-card">
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="price-cell">{{ formatPrice(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS_COLOR[row.status]" size="small" effect="dark">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" :formatter="(_, __, val) => formatDate(val)" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="$router.push(`/orders/${row.id}`)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              text type="success" size="small"
              @click="handleShip(row.id)"
            >
              发货
            </el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              text type="danger" size="small"
              @click="handleCancel(row.id)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadOrders"
        @size-change="loadOrders"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, User, Download } from '@element-plus/icons-vue'
import { getOrderList, shipOrder, cancelOrder, exportOrders } from '@/api/admin'
import { formatPrice, formatDate } from '@/utils/format'
import { ORDER_STATUS_MAP, ORDER_STATUS_COLOR } from '@shared/constants'
import type { Order } from '@shared/types/order'

const orders = ref<Order[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const userId = ref('')
const status = ref<number | ''>('')

async function loadOrders() {
  loading.value = true
  try {
    const res = await getOrderList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      userId: userId.value ? Number(userId.value) : undefined,
      status: status.value || undefined,
    })
    orders.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const exporting = ref(false)
const exportErrorVisible = ref(false)
const exportErrorMessage = ref('')

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportOrders({
      keyword: keyword.value || undefined,
      userId: userId.value ? Number(userId.value) : undefined,
      status: status.value || undefined,
    })

    if (blob.type === 'application/json') {
      const text = await blob.text()
      try {
        const json = JSON.parse(text)
        exportErrorMessage.value = json.message || '导出失败'
      } catch {
        exportErrorMessage.value = '导出失败，请重试'
      }
      exportErrorVisible.value = true
      return
    }

    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = ''
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    exportErrorMessage.value = e?.response?.data?.message
      || e?.message
      || '导出失败，请检查网络后重试'
    exportErrorVisible.value = true
  } finally {
    exporting.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadOrders()
}

async function handleShip(id: number) {
  await ElMessageBox.confirm('确认发货？', '提示', { type: 'warning' })
  await shipOrder(id)
  ElMessage.success('已发货')
  loadOrders()
}

async function handleCancel(id: number) {
  await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  await cancelOrder(id)
  ElMessage.success('已取消')
  loadOrders()
}

onMounted(() => { loadOrders() })
</script>

<style lang="scss" scoped>
.order-manage { /* max-width handled by .content-wrapper */ }

.page-title {
  margin-bottom: 24px;
}

.toolbar {
  &-search { width: 260px; }
  &-select { width: 160px; }
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.price-cell {
  color: #e08880;
  font-weight: 700;
  font-size: 14px;
}
</style>
