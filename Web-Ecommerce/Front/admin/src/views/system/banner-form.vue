<template>
  <div class="banner-form">
    <div class="page-header">
      <h1 class="page-title">{{ isEdit ? '编辑轮播' : '新增轮播' }}</h1>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入轮播标题" />
      </el-form-item>

      <el-form-item label="图片" prop="imageUrl">
        <div class="upload-area">
          <el-image
            v-if="form.imageUrl"
            :src="form.imageUrl"
            fit="cover"
            class="upload-preview"
          />
          <el-upload
            action="/api/admin/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
          >
            <el-button :type="form.imageUrl ? '' : 'primary'">
              {{ form.imageUrl ? '更换图片' : '上传图片' }}
            </el-button>
          </el-upload>
          <el-button v-if="form.imageUrl" text type="danger" @click="form.imageUrl = ''">移除</el-button>
        </div>
      </el-form-item>

      <el-form-item label="跳转链接">
        <el-input v-model="form.linkUrl" placeholder="请输入跳转链接（选填）" />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" placeholder="越小越靠前" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '新增轮播' }}
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
import { createBanner, updateBanner, getBannerById } from '@/api/admin'
import { ADMIN_TOKEN_KEY } from '@shared/constants'

const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem(ADMIN_TOKEN_KEY) || ''}` }

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
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  status: 1,
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
}

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getBannerById(id.value)
    const b = res.data
    form.value = {
      title: b.title,
      imageUrl: b.imageUrl,
      linkUrl: b.linkUrl || '',
      sortOrder: b.sortOrder,
      status: b.status,
    }
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
})

function handleUploadSuccess(response: any) {
  // el-upload 返回原始 JSON: { code, message, data: { url } }
  const url = response?.data?.url || response?.url || ''
  if (!url) {
    ElMessage.error('图片上传异常，未获取到文件地址')
    return
  }
  form.value.imageUrl = url
  ElMessage.success('上传成功')
}

function handleUploadError() {
  ElMessage.error('图片上传失败，请重试')
}

function beforeUpload(file: File) {
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
  }
  return isLt5M
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBanner(id.value, form.value)
      ElMessage.success('已更新')
    } else {
      await createBanner(form.value)
      ElMessage.success('已创建')
    }
    router.push('/system/banners')
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.banner-form {
  max-width: 740px;
}

.page-header {
  margin-bottom: 28px;
}

.upload-area {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.upload-preview {
  width: 200px;
  height: 100px;
  border-radius: var(--org-radius-md);
  border: 1px solid var(--org-border);
  object-fit: cover;
}
</style>
