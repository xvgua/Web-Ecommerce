<template>
  <div class="user-manage">
    <h1 class="page-title">用户管理</h1>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索用户名/手机号" clearable maxlength="100" class="tbar-input" @input="debouncedSearch" @keyup.enter="handleSearch">
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
              <el-avatar :size="36">
                <span style="font-size:15px">{{ (row.nickname || row.username)?.[0]?.toUpperCase() }}</span>
              </el-avatar>
              <div>
                <div class="user-cell__name">{{ row.nickname || row.username }}</div>
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
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useDebounceFn } from '@vueuse/core'
import { getUserList, toggleUserStatus } from '@/api/admin'
import { formatDate } from '@/utils/format'
import type { User } from '@shared/types/user'

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

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
.user-manage { max-width: 1400px; }

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
}

.tbar-input { width: 260px; }

.table-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}

.account-id {
  font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  font-weight: 600;
  color: #303133;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  &__name { font-size: 14px; font-weight: 500; }
  &__id   { font-size: 12px; color: #999; font-family: 'SF Mono', 'Menlo', 'Consolas', monospace; }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
