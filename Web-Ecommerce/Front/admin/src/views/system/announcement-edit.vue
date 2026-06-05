<template>
  <div class="announcement-edit">
    <div class="page-header">
      <h1 class="page-title">{{ isEdit ? '编辑公告' : '发布公告' }}</h1>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" v-loading="loading">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入公告标题" />
      </el-form-item>

      <el-form-item label="内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          maxlength="2000"
          show-word-limit
          placeholder="请输入公告内容（纯文本）"
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="草稿" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="已下架" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="级别">
            <el-select v-model="form.level">
              <el-option label="普通 (info)" value="info" />
              <el-option label="提醒 (warning)" value="warning" />
              <el-option label="重要 (important)" value="important" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" placeholder="越大越靠前" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '发布公告' }}
        </el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { createAnnouncement, updateAnnouncement, getAnnouncementById } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)

const id = computed(() => {
  const val = route.query.id
  return val ? Number(val) : 0
})

const isEdit = computed(() => id.value > 0)

const form = ref({
  title: '',
  content: '',
  status: 1,
  level: 'info' as string,
  sortOrder: 0,
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getAnnouncementById(id.value)
    const a = res.data
    form.value = { title: a.title, content: a.content, status: a.status, level: a.level, sortOrder: a.sortOrder }
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAnnouncement(id.value, form.value)
      ElMessage.success('已更新')
    } else {
      await createAnnouncement(form.value)
      ElMessage.success('已发布')
    }
    router.push('/system/announcements')
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.announcement-edit {
  max-width: 840px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--org-text);
  letter-spacing: -.4px;
}
</style>
