<template>
  <div class="user-manage">
    <h1 class="page-title">用户管理</h1>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索用户ID / 手机号 / 昵称" clearable maxlength="100" class="tbar-input" @input="debouncedSearch" @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="账号ID" width="110" align="center">
          <template #default="{ row }">
            <span class="account-id">{{ row.accountId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatar || undefined">
                <span style="font-size:15px">{{ row.username?.[0]?.toUpperCase() }}</span>
              </el-avatar>
              <div>
                <div class="user-cell__name">{{ row.username }}</div>
                <div class="user-cell__id">{{ row.accountId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" :formatter="(_, __, val) => formatDate(val)" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              text type="danger" size="small"
              @click="handleToggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              text type="success" size="small"
              @click="handleToggleStatus(row)"
            >
              启用
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
        @current-change="loadUsers"
        @size-change="loadUsers"
      />
    </div>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="560px" :close-on-click-modal="false">
      <template v-if="detailUser">
        <div class="detail-header">
          <el-avatar :size="64" :src="detailUser.avatar || undefined">
            <span style="font-size:26px">{{ detailUser.username?.[0]?.toUpperCase() }}</span>
          </el-avatar>
          <div class="detail-header__info">
            <div class="detail-header__name">{{ detailUser.username }}</div>
            <div class="detail-header__id">账号ID: {{ detailUser.accountId }}</div>
            <el-tag :type="detailUser.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
              {{ detailUser.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </div>
        </div>
        <el-divider />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户ID">{{ detailUser.id }}</el-descriptions-item>
          <el-descriptions-item label="账号ID">{{ detailUser.accountId }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detailUser.username }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ detailUser.gender === 1 ? '男' : detailUser.gender === 0 ? '女' : '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="简介">{{ detailUser.intro || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(detailUser.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useDebounceFn } from '@vueuse/core'
import { getUserList, getUserById, toggleUserStatus } from '@/api/admin'
import { formatDate } from '@/utils/format'
import type { User } from '@shared/types/user'

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

const detailVisible = ref(false)
const detailUser = ref<User | null>(null)

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
    })
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadUsers()
}

const debouncedSearch = useDebounceFn(() => {
  handleSearch()
}, 300)

async function handleViewDetail(row: User) {
  detailVisible.value = true
  detailUser.value = null
  try {
    const res = await getUserById(row.id)
    detailUser.value = res.data
  } catch {
    detailVisible.value = false
  }
}

async function handleToggleStatus(row: User) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', { type: 'warning' })
  await toggleUserStatus(row.id, newStatus)
  ElMessage.success(`已${action}`)
  loadUsers()
}

onMounted(() => { loadUsers() })
</script>

<style lang="scss" scoped>
.user-manage { /* max-width handled by .content-wrapper */ }

.page-title {
  margin-bottom: 24px;
}

.tbar-input { width: 280px; }

.account-id {
  font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  font-weight: 600;
  color: var(--org-text);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  &__name { font-size: 14px; font-weight: 600; color: var(--org-text); }
  &__id   { font-size: 12px; color: var(--org-text-muted); font-family: 'SF Mono', 'Menlo', 'Consolas', monospace; }
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;

  &__info {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__name {
    font-size: 18px;
    font-weight: 700;
    color: var(--org-text);
  }

  &__id {
    font-size: 13px;
    color: var(--org-text-muted);
    font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  }
}
</style>
