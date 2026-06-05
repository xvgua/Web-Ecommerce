<template>
  <div class="banner-manage">
    <div class="page-header">
      <h1 class="page-title">轮播管理</h1>
      <div class="page-header__right">
        <el-input
          v-model="keyword"
          placeholder="搜索标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="$router.push('/system/banners/create')">新增轮播</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="list" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="图片" width="160">
        <template #default="{ row }">
          <el-image
            :src="row.imageUrl"
            fit="cover"
            style="width: 140px; height: 60px; border-radius: 4px"
            :preview-src-list="[row.imageUrl]"
          />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="linkUrl" label="跳转链接" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="(val: boolean) => handleToggleStatus(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/system/banners/create?id=${row.id}`)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBannerList, deleteBanner, toggleBannerStatus } from '@/api/admin'
import type { Banner } from '@shared/types'

const list = ref<Banner[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function fetchList() {
  loading.value = true
  try {
    const res = await getBannerList({ page: page.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = res.data.records
    total.value = res.data.total
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function handleSearch() {
  page.value = 1
  fetchList()
}

async function handleToggleStatus(row: Banner, val: boolean) {
  try {
    await ElMessageBox.confirm(
      `确定${val ? '启用' : '禁用'}轮播"${row.title}"？`,
      '确认操作',
      { type: 'warning' },
    )
  } catch { return }
  try {
    await toggleBannerStatus(row.id, val ? 1 : 0)
    row.status = val ? 1 : 0
    ElMessage.success(`已${val ? '启用' : '禁用'}`)
  } catch { /* handled by interceptor */ }
}

async function handleDelete(row: Banner) {
  try {
    await ElMessageBox.confirm(`确定删除轮播"${row.title}"？`, '确认删除', { type: 'warning' })
  } catch { return }
  try {
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    await fetchList()
  } catch { /* handled by interceptor */ }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.banner-manage {
  /* max-width handled by .content-wrapper */
}

.page-header {
  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
