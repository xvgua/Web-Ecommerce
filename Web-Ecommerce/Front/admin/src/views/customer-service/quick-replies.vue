<template>
  <div class="qr-page">
    <div class="page-title-row">
      <h1 class="page-title">快捷回复管理</h1>
      <el-button type="primary" @click="openForm(null)">新增回复</el-button>
    </div>

    <div class="table-card">
      <el-table :data="replies" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="content" label="回复内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="dark">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="handleToggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑快捷回复' : '新增快捷回复'"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="内部识别用" maxlength="100" />
        </el-form-item>
        <el-form-item label="回复内容" required>
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="回复内容..." />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQuickReplies, createQuickReply, updateQuickReply, deleteQuickReply, toggleQuickReplyStatus } from '@/api/chat'
import type { QuickReply } from '@shared/types/chat'

const replies = ref<QuickReply[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  title: '',
  content: '',
  sortOrder: 0,
})

function resetForm() {
  form.value = { title: '', content: '', sortOrder: 0 }
  editingId.value = null
}

function openForm(reply: QuickReply | null) {
  if (reply) {
    editingId.value = reply.id
    form.value = { title: reply.title, content: reply.content, sortOrder: reply.sortOrder }
  } else {
    resetForm()
  }
  dialogVisible.value = true
}

async function loadReplies() {
  loading.value = true
  try {
    const res = await getQuickReplies()
    replies.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('请填写标题和回复内容')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateQuickReply(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await createQuickReply(form.value)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadReplies()
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

async function handleToggle(reply: QuickReply) {
  const newStatus = reply.status === 1 ? 0 : 1
  await toggleQuickReplyStatus(reply.id, newStatus)
  reply.status = newStatus
  ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该快捷回复吗？', '提示', { type: 'warning' })
  await deleteQuickReply(id)
  ElMessage.success('已删除')
  loadReplies()
}

onMounted(() => { loadReplies() })
</script>

<style lang="scss" scoped>
.qr-page { padding: 0; }
.page-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title { font-size: 20px; font-weight: 600; margin: 0; }
.table-card { background: #fff; border-radius: 8px; padding: 4px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
</style>
