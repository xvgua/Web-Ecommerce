<template>
  <div class="refund-manage">
    <h1 class="page-title">退款管理</h1>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索订单号"
        clearable
        class="toolbar-search"
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select
        v-model="refundStatus"
        placeholder="退款状态"
        clearable
        class="toolbar-select"
        @change="handleSearch"
      >
        <el-option
          v-for="(text, val) in REFUND_STATUS_MAP"
          :key="val"
          :label="text"
          :value="Number(val)"
        />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-card">
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column label="退款类型" width="100" align="center">
          <template #default="{ row }">
            {{ row.refundType === 1 ? '仅退款' : '退货退款' }}
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="120" align="center">
          <template #default="{ row }">
            <span class="price-cell">{{ formatPrice(row.refundAmount || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款原因" width="140">
          <template #default="{ row }">
            {{ row.refundReasonText || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="退款状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="REFUND_STATUS_COLOR[row.refundStatus]" size="small" effect="dark">
              {{ row.refundStatusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.refundApplyTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.refundStatus === 0"
              text type="success" size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.refundStatus === 0"
              text type="danger" size="small"
              @click="handleReject(row)"
            >
              拒绝
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
        @current-change="loadList"
        @size-change="loadList"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="退款详情" width="640px">
      <div v-if="detailOrder" class="refund-detail-dialog">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="退款类型">
            {{ detailOrder.refundType === 1 ? '仅退款' : '退货退款' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span class="price-cell">{{ formatPrice(detailOrder.refundAmount || 0) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ detailOrder.refundReasonText }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">
            <el-tag :type="REFUND_STATUS_COLOR[detailOrder.refundStatus!]" size="small" effect="dark">
              {{ detailOrder.refundStatusText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="补充说明" :span="2">
            {{ detailOrder.refundDesc || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDate(detailOrder.refundApplyTime) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ detailOrder.refundDealTime ? formatDate(detailOrder.refundDealTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailOrder.refundRejectReason" label="拒绝原因" :span="2">
            <span style="color: #f56c6c">{{ detailOrder.refundRejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="detailOrder.refundItems?.length" class="detail-items">
          <h4>退款商品</h4>
          <div v-for="item in detailOrder.refundItems" :key="item.id" class="detail-item">
            <span>{{ item.productName }}</span>
            <span>{{ formatPrice(item.price) }} x{{ item.quantity }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝退款" width="480px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请填写拒绝原因"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { formatPrice, formatDate } from '@/utils/format'
import { REFUND_STATUS_MAP, REFUND_STATUS_COLOR } from '@shared/constants'
import type { Order } from '@shared/types/order'
import request from '@/api/request'
import type { ApiResponse, PageResponse } from '@shared/types'

const orders = ref<Order[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const refundStatus = ref<number | ''>('')

const detailVisible = ref(false)
const detailOrder = ref<Order | null>(null)
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectSubmitting = ref(false)
const pendingRejectId = ref<number | null>(null)

async function loadList() {
  loading.value = true
  try {
    const res = await request.get<ApiResponse<PageResponse<Order>>>('/admin/orders/refunds', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        keyword: keyword.value || undefined,
        refundStatus: refundStatus.value || undefined,
      },
    })
    orders.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadList()
}

async function showDetail(row: Order) {
  try {
    const res = await request.get<ApiResponse<Order>>(`/admin/orders/refunds/${row.id}`)
    detailOrder.value = res.data
    detailVisible.value = true
  } catch { /* handled */ }
}

function handleApprove(row: Order) {
  ElMessageBox.confirm('确认通过该退款申请吗？通过后将恢复库存。', '提示', { type: 'warning' }).then(async () => {
    await request.put(`/admin/orders/refunds/${row.id}/audit`, { approved: true })
    ElMessage.success('退款申请已通过')
    loadList()
  }).catch(() => {})
}

function handleReject(row: Order) {
  pendingRejectId.value = row.id
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectSubmitting.value = true
  try {
    await request.put(`/admin/orders/refunds/${pendingRejectId.value}/audit`, {
      approved: false,
      rejectReason: rejectReason.value,
    })
    ElMessage.success('已拒绝退款申请')
    rejectVisible.value = false
    loadList()
  } finally {
    rejectSubmitting.value = false
  }
}

onMounted(() => { loadList() })
</script>

<style lang="scss" scoped>
.refund-manage { /* max-width handled by .content-wrapper */ }

.page-title {
  margin-bottom: 24px;
}

.toolbar {
  &-search { width: 260px; }
  &-select { width: 160px; }
}

.price-cell {
  color: #e08880;
  font-weight: 700;
  font-size: 14px;
}

.detail-items {
  margin-top: 20px;

  h4 {
    font-size: 15px;
    font-weight: 700;
    margin-bottom: 12px;
    color: var(--org-text);
  }
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--org-border-soft);
  font-size: 13px;
  color: var(--org-text-secondary);
  font-weight: 500;

  &:last-child { border-bottom: none; }
}
</style>
