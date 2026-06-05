<template>
  <div class="announcement-manage">
    <div class="page-header">
      <h1 class="page-title">公告管理</h1>
      <el-button type="primary" @click="$router.push('/system/announcements/edit')">发布公告</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="levelTag(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/system/announcements/edit?id=${row.id}`)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAnnouncementList, deleteAnnouncement } from '@/api/admin'
import { formatDate } from '@/utils/format'
import type { Announcement } from '@shared/types'

const list = ref<Announcement[]>([])
const loading = ref(false)

function statusLabel(s: number) {
  return { 0: '草稿', 1: '已发布', 2: '已下架' }[s] || '未知'
}

function statusTag(s: number) {
  return { 0: 'info', 1: 'success', 2: 'warning' }[s] || 'info'
}

function levelLabel(l: string) {
  return { info: '普通', warning: '提醒', important: '重要' }[l] || l
}

function levelTag(l: string) {
  return { info: 'info', warning: 'warning', important: 'danger' }[l] || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getAnnouncementList({ page: 1, pageSize: 100 })
    list.value = res.data.records
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

async function handleDelete(row: Announcement) {
  try {
    await ElMessageBox.confirm(`确定删除公告"${row.title}"？`, '确认删除', { type: 'warning' })
  } catch { return }
  try {
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    await fetchList()
  } catch { /* handled by interceptor */ }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.announcement-manage {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--org-text);
  letter-spacing: -.4px;
}
</style>
