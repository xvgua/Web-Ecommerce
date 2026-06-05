<template>
  <div class="seckill-manage">
    <el-card>
      <div class="page-header">
        <h3>秒杀活动管理</h3>
        <el-button type="primary" @click="handleCreate">新增秒杀活动</el-button>
      </div>

      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="活动名称" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" clearable placeholder="全部" @change="loadData">
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="keyword = ''; statusFilter = undefined; loadData()">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="活动名称" min-width="160" />
        <el-table-column label="活动时间" width="340">
          <template #default="{ row }">
            <div>{{ row.startTime }} ~ {{ row.endTime }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="秒杀商品数" width="100">
          <template #default="{ row }">
            {{ row.products?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="loadData"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSeckillActivityList, deleteSeckillActivity } from '@/api/admin'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

function statusText(s: number) {
  return { 0: '未开始', 1: '进行中', 2: '已结束' }[s] || '未知'
}

function statusTag(s: number) {
  return { 0: 'info', 1: 'success', 2: '' }[s] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSeckillActivityList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value,
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { /* handled */ } finally { loading.value = false }
}

function handleCreate() {
  router.push('/seckill/create')
}

function handleEdit(row: any) {
  router.push(`/seckill/${row.id}/edit`)
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该秒杀活动吗？', '提示', { type: 'warning' })
    await deleteSeckillActivity(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled or handled */ }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.seckill-manage { max-width: 1400px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: var(--org-text);
  }
}

.search-form {
  margin-bottom: 20px;

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--org-text-secondary);
  }
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
