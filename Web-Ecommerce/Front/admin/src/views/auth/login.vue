<template>
  <div class="admin-login">
    <div class="login-card">
      <div class="login-card__brand">
        <div class="login-card__icon">
          <svg viewBox="0 0 80 80" width="48" height="48" xmlns="http://www.w3.org/2000/svg">
            <rect width="80" height="80" rx="16" fill="rgba(255,255,255,.2)" />
            <text x="40" y="52" text-anchor="middle" font-size="38" fill="#fff">&#x1F6E1;</text>
          </svg>
        </div>
        <h2>管理后台</h2>
        <p>Ecommerce Marketplace Admin</p>
      </div>
      <div class="login-card__form">
        <h3>管理员登录</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入管理员账号"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              native-type="submit"
              class="login-btn"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  loading.value = true
  try {
    await adminStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 24px;
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0,0,0,.35);

  &__brand {
    background: linear-gradient(135deg, #1a1a2e, #16213e);
    padding: 36px 32px;
    text-align: center;
    color: #fff;

    h2 { font-size: 20px; font-weight: 700; margin-top: 12px; }
    p  { font-size: 12px; opacity: .55; margin-top: 4px; letter-spacing: 1px; }
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  &__form {
    padding: 32px 36px;

    h3 {
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 24px;
      color: #1a1a2e;
    }
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  border: none;

  &:hover {
    background: linear-gradient(135deg, #2a2a4e, #26315e);
  }
}
</style>
