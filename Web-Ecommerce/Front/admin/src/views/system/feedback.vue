<template>
  <div class="feedback-manage">
    <div class="page-header">
      <h2>用户反馈管理</h2>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="query.type" placeholder="反馈类型" clearable @change="handleSearch">
          <el-option label="问题反馈" :value="1" />
          <el-option label="功能建议" :value="2" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable @change="handleSearch">
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已解决" :value="2" />
          <el-option label="已关闭" :value="3" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索标题"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <div class="batch-row">
        <el-button :disabled="!selectedIds.length" @click="handleBatchClose">批量关闭</el-button>
        <el-button :disabled="!selectedIds.length" type="danger" @click="handleBatchDelete">批量删除</el-button>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" @selection-change="handleSelectionChange" stripe>
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'danger' : 'primary'" size="small">
              {{ FEEDBACK_TYPE_MAP[row.type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="FEEDBACK_STATUS_COLOR[row.status] as any" size="small" effect="plain">
              {{ FEEDBACK_STATUS_MAP[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openReplyDialog(row)">处理</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- Reply Dialog -->
    <el-dialog v-model="replyVisible" title="处理反馈" width="560px" :close-on-click-modal="false">
      <div class="reply-feedback-info" v-if="currentFeedback">
        <div class="info-item">
          <span class="info-label">反馈类型：</span>
          <el-tag :type="currentFeedback.type === 1 ? 'danger' : 'primary'" size="small">
            {{ FEEDBACK_TYPE_MAP[currentFeedback.type] }}
          </el-tag>
        </div>
        <div class="info-item">
          <span class="info-label">用户：</span>
          <span>{{ currentFeedback.username }} ({{ currentFeedback.userEmail }})</span>
        </div>
        <div class="info-item">
          <span class="info-label">标题：</span>
          <span>{{ currentFeedback.title }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">内容：</span>
          <div class="info-content">{{ currentFeedback.content }}</div>
        </div>
        <div class="info-item" v-if="currentFeedback.contact">
          <span class="info-label">联系方式：</span>
          <span>{{ currentFeedback.contact }}</span>
        </div>
        <div class="info-item" v-if="imageList.length">
          <span class="info-label">截图：</span>
          <div class="info-images">
            <el-image
              v-for="(img, i) in imageList"
              :key="i"
              :src="img"
              fit="cover"
              :preview-src-list="imageList"
              :initial-index="i"
              class="info-img"
            />
          </div>
        </div>
        <div class="info-item">
          <span class="info-label">提交时间：</span>
          <span>{{ currentFeedback.createTime }}</span>
        </div>
      </div>

      <el-divider />

      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="replyForm.status">
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="回复内容" prop="adminReply">
          <el-input
            v-model="replyForm.adminReply"
            type="textarea"
            placeholder="请输入回复内容..."
            maxlength="2000"
            show-word-limit
            :rows="4"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getFeedbackList, getFeedbackDetail, replyFeedback, deleteFeedback } from '@/api/admin'
import { FEEDBACK_TYPE_MAP, FEEDBACK_STATUS_MAP, FEEDBACK_STATUS_COLOR, ADMIN_PAGE_SIZE } from '@shared/constants'
import type { Feedback } from '@shared/types/feedback'

const loading = ref(false)
const list = ref<Feedback[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const query = reactive({
  page: 1,
  pageSize: ADMIN_PAGE_SIZE,
  type: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: '',
})

function handleSelectionChange(rows: Feedback[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function loadList() {
  loading.value = true
  try {
    const res = await getFeedbackList({
      page: query.page,
      pageSize: query.pageSize,
      type: query.type,
      status: query.status,
      keyword: query.keyword || undefined,
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该反馈吗？', '提示', { type: 'warning' })
  await deleteFeedback(id)
  ElMessage.success('已删除')
  loadList()
}

async function handleBatchClose() {
  await ElMessageBox.confirm(`确定要批量关闭选中的 ${selectedIds.value.length} 条反馈吗？`, '提示', { type: 'warning' })
  for (const id of selectedIds.value) {
    try {
      await replyFeedback(id, { status: 3, adminReply: '批量关闭' })
    } catch { /* continue */ }
  }
  ElMessage.success('批量关闭完成')
  selectedIds.value = []
  loadList()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定要批量删除选中的 ${selectedIds.value.length} 条反馈吗？此操作不可恢复。`, '警告', { type: 'warning' })
  for (const id of selectedIds.value) {
    try {
      await deleteFeedback(id)
    } catch { /* continue */ }
  }
  ElMessage.success('批量删除完成')
  selectedIds.value = []
  loadList()
}

// ---- Reply Dialog ----
const replyVisible = ref(false)
const replying = ref(false)
const replyFormRef = ref<FormInstance>()
const currentFeedback = ref<Feedback | null>(null)

const replyForm = reactive({
  status: 1,
  adminReply: '',
})

const replyRules: FormRules = {
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  adminReply: [{ required: true, message: '请输入回复内容', trigger: 'blur' }],
}

const imageList = computed(() => {
  if (!currentFeedback.value?.images) return []
  if (Array.isArray(currentFeedback.value.images)) {
    return currentFeedback.value.images
  }
  return (currentFeedback.value.images as string).split(',').filter(Boolean)
})

async function openReplyDialog(row: Feedback) {
  try {
    const res = await getFeedbackDetail(row.id)
    currentFeedback.value = res.data
    replyForm.status = 1
    replyForm.adminReply = ''
    replyVisible.value = true
  } catch {
    // fallback to row data
    currentFeedback.value = row
    replyForm.status = 1
    replyForm.adminReply = ''
    replyVisible.value = true
  }
}

async function handleReply() {
  const valid = await replyFormRef.value?.validate()
  if (!valid || !currentFeedback.value) return

  replying.value = true
  try {
    await replyFeedback(currentFeedback.value.id, {
      status: replyForm.status,
      adminReply: replyForm.adminReply,
    })
    ElMessage.success('已回复')
    replyVisible.value = false
    loadList()
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.feedback-manage {
  .page-header {
    margin-bottom: 16px;
    h2 { font-size: 20px; font-weight: 600; }
  }
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.batch-row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.table-card {
  .table-pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}

.reply-feedback-info {
  .info-item {
    margin-bottom: 10px;
    font-size: 14px;
    display: flex;
    align-items: flex-start;
  }

  .info-label {
    color: #999;
    flex-shrink: 0;
    min-width: 80px;
  }

  .info-content {
    color: #333;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .info-images {
    display: flex;
    gap: 8px;
  }

  .info-img {
    width: 60px;
    height: 60px;
    border-radius: 4px;
    border: 1px solid #eee;
  }
}
</style>
