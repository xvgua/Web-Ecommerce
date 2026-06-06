<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>

    <div class="profile-grid">
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>管理员信息</span>
            <el-tag :type="profile?.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
              {{ profile?.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">
            <span class="info-value">{{ profile?.username || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag size="small">{{ profile?.role || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ profile?.createTime ? formatDate(profile.createTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="profile-card">
        <template #header>
          <span>修改密码</span>
        </template>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          label-position="left"
          size="large"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="form.oldPassword"
              type="password"
              show-password
              placeholder="请输入旧密码"
              maxlength="20"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              show-password
              placeholder="6-20个字符"
              maxlength="20"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
              maxlength="20"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ADMIN_TOKEN_KEY } from '@shared/constants'
import { getAdminProfile, changeAdminPassword } from '@/api/admin'
import { formatDate } from '@/utils/format'
import type { AdminInfo } from '@/api/admin'

const profile = ref<AdminInfo | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: any, value: string, callback: (err?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在6-20个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function loadProfile() {
  try {
    const res = await getAdminProfile()
    profile.value = res.data
  } catch {
    // handled by interceptor
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await changeAdminPassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    // Clear token and redirect to login
    localStorage.removeItem(ADMIN_TOKEN_KEY)
    window.location.href = '/login'
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => { loadProfile() })
</script>

<style lang="scss" scoped>
.profile-page {
  max-width: 640px;
}

.page-title {
  margin-bottom: 24px;
}

.profile-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  :deep(.el-card__header) {
    border-bottom: 1px solid var(--border-l2);
    padding: 16px 20px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-value {
  font-weight: var(--font-weight-bold);
  color: var(--org-text);
}
</style>
