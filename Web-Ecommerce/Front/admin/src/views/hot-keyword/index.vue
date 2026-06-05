<template>
  <div class="hot-keyword-page">
    <div class="page-header">
      <h1>热门搜索词</h1>
      <div class="page-header__actions">
        <el-button type="success" :loading="computing" @click="handleCompute">刷新统计</el-button>
        <el-button type="primary" @click="handleCreate">新增关键词</el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索关键词..."
        clearable
        style="width: 240px"
        @input="handleSearch"
        @clear="handleSearch"
      />
    </div>

    <el-card shadow="never">
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="keyword" label="关键词" min-width="160" />
        <el-table-column prop="searchCount" label="搜索次数" width="110" align="center" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isManual ? 'warning' : 'info'" size="small">
              {{ row.isManual ? '手动' : '自动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="!!row.isPinned"
              size="small"
              @change="handleTogglePin(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="!!row.status"
              size="small"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑关键词' : '新增关键词'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="关键词" prop="keyword">
          <el-input v-model="form.keyword" placeholder="请输入关键词" maxlength="50" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isPinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getHotKeywordList,
  createHotKeyword,
  updateHotKeyword,
  deleteHotKeyword,
  toggleHotKeywordPin,
  toggleHotKeywordStatus,
  computeHotKeywords,
} from '@/api/admin'
import type { HotKeyword } from '@shared/types/product'
import { useDebounceFn } from '@vueuse/core'

const records = ref<HotKeyword[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number>(0)
const computing = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  keyword: '',
  isPinned: 0,
  sortOrder: 0,
})

const rules: FormRules = {
  keyword: [
    { required: true, message: '请输入关键词', trigger: 'blur' },
    { max: 50, message: '关键词不能超过50个字符', trigger: 'blur' },
  ],
}

async function loadData() {
  loading.value = true
  try {
    const res = await getHotKeywordList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = useDebounceFn(() => {
  page.value = 1
  loadData()
}, 300)

function handleCreate() {
  isEdit.value = false
  editingId.value = 0
  form.keyword = ''
  form.isPinned = 0
  form.sortOrder = 0
  dialogVisible.value = true
}

function handleEdit(row: HotKeyword) {
  isEdit.value = true
  editingId.value = row.id
  form.keyword = row.keyword
  form.isPinned = row.isPinned
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateHotKeyword(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createHotKeyword({ ...form })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该关键词？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteHotKeyword(id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleTogglePin(row: HotKeyword) {
  await toggleHotKeywordPin(row.id)
  row.isPinned = row.isPinned ? 0 : 1
  ElMessage.success(row.isPinned ? '已置顶' : '已取消置顶')
}

async function handleToggleStatus(row: HotKeyword) {
  await toggleHotKeywordStatus(row.id)
  row.status = row.status ? 0 : 1
  ElMessage.success('状态已更新')
}

async function handleCompute() {
  computing.value = true
  try {
    await computeHotKeywords(30, 20)
    ElMessage.success('热门关键词统计完成')
    loadData()
  } finally {
    computing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.hot-keyword-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: var(--org-text);
    letter-spacing: -.4px;
  }

  &__actions {
    display: flex;
    gap: 10px;
  }
}

.toolbar {
  margin-bottom: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>
