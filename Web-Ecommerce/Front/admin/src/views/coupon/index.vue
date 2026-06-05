<template>
  <div class="coupon-manage">
    <div class="page-header">
      <h1 class="page-title">优惠券管理</h1>
      <el-button type="primary" @click="$router.push('/coupons/create')">
        <el-icon><Plus /></el-icon> 新增优惠券
      </el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索券名称" clearable class="toolbar-search" @input="debouncedSearch" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="typeFilter" placeholder="优惠类型" clearable @change="handleSearch" class="toolbar-select">
        <el-option label="满减券" :value="1" />
        <el-option label="折扣券" :value="2" />
        <el-option label="免邮券" :value="3" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable @change="handleSearch" class="toolbar-select-sm">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-card">
      <el-table :data="coupons" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="券名称" min-width="160">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.isLarge" type="danger" size="small" class="ml-8">大额券</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag[row.type]" size="small">{{ typeLabel[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠" width="100">
          <template #default="{ row }">
            <template v-if="row.type === 2">
              <span class="price-cell">{{ formatDiscount(row.discount) }}折</span>
            </template>
            <template v-else-if="row.type === 3">
              <span class="price-cell">免邮</span>
            </template>
            <template v-else>
              <span class="price-cell">¥{{ row.discount }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="门槛" width="100">
          <template #default="{ row }">
            {{ row.minAmount > 0 ? `满¥${row.minAmount}` : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column label="库存" width="140">
          <template #default="{ row }">
            <div class="stock-info">
              <span>已领 {{ row.totalQty - row.remainQty }} / {{ row.totalQty }}</span>
              <el-progress
                :percentage="row.totalQty > 0 ? Math.round((row.totalQty - row.remainQty) / row.totalQty * 100) : 0"
                :stroke-width="4"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="适用范围" width="100">
          <template #default="{ row }">
            {{ scopeLabel[row.scopeType] || '通用' }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val: boolean) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="$router.push(`/coupons/${row.id}/edit`)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
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
        @current-change="loadCoupons"
        @size-change="loadCoupons"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useDebounceFn } from '@vueuse/core'
import { getCouponList, deleteCoupon, toggleCouponStatus } from '@/api/admin'
import { formatPrice } from '@/utils/format'
import type { Coupon } from '@shared/types/coupon'

const coupons = ref<Coupon[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const typeFilter = ref<number | ''>('')
const statusFilter = ref<number | ''>('')

const typeLabel: Record<number, string> = { 1: '满减券', 2: '折扣券', 3: '免邮券' }
const typeTag: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'success' }
const scopeLabel: Record<number, string> = { 1: '通用', 2: '指定分类', 3: '指定商品' }

function formatDate(dateStr: string) {
  return dateStr ? dateStr.substring(0, 10) : ''
}

function formatDiscount(discount: number) {
  const value = Math.round(discount * 1000) / 100
  return value % 1 === 0 ? value.toFixed(0) : value.toFixed(1)
}

async function loadCoupons() {
  loading.value = true
  try {
    const res = await getCouponList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      type: typeFilter.value || undefined,
      status: statusFilter.value !== '' ? (statusFilter.value as number) : undefined,
    })
    coupons.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadCoupons()
}

const debouncedSearch = useDebounceFn(() => { handleSearch() }, 300)

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该优惠券吗？', '提示', { type: 'warning' })
  await deleteCoupon(id)
  ElMessage.success('已删除')
  loadCoupons()
}

async function handleToggleStatus(row: Coupon, val: boolean) {
  const newStatus = val ? 1 : 0
  const label = val ? '启用' : '停用'
  await ElMessageBox.confirm(`确定要${label}该优惠券吗？`, '提示', { type: 'warning' })
  await toggleCouponStatus(row.id, newStatus)
  row.status = newStatus
  ElMessage.success(`已${label}`)
}

onMounted(() => { loadCoupons() })
</script>

<style lang="scss" scoped>
.coupon-manage { max-width: 1400px; }

.toolbar {
  &-search { width: 240px; }
  &-select { width: 140px; }
  &-select-sm { width: 110px; }
}

.price-cell {
  color: #e08880;
  font-weight: 700;
}

.date-text {
  font-size: 12px;
  color: var(--org-text-secondary);
  font-weight: 500;
}

.stock-info {
  font-size: 12px;
  color: var(--org-text-secondary);
  font-weight: 500;
}

.ml-8 { margin-left: 8px; }
</style>
