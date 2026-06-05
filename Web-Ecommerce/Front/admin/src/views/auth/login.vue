<template>
  <div class="admin-login">
    <div class="login-scene" />

    <div class="login-card">
      <div class="login-card__brand">
        <div class="login-card__logo">
          <svg viewBox="0 0 52 52" width="52" height="52" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="login-logo-grad" x1="6" y1="6" x2="46" y2="46">
                <stop offset="0%" stop-color="#3964FE" />
                <stop offset="100%" stop-color="#5686FE" />
              </linearGradient>
            </defs>
            <rect x="6" y="6" width="40" height="40" rx="16" fill="#3964FE" />
            <path d="M26 17c-4 4-7 9-7 9h5v7h3v-7h5s-2-5-6-9z" fill="#fff" opacity=".95" />
            <circle cx="26" cy="22" r="3" fill="#fff" opacity=".5" />
          </svg>
        </div>
        <h2>乐购管理后台</h2>
        <p>Ecommerce Marketplace</p>
      </div>

      <div class="login-card__form">
        <h3>管理员登录</h3>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
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
  background: #0F1115;
  position: relative;
  padding: 24px;
  overflow: hidden;
}

/* ── Subtle dot pattern (no blobs) ── */
.login-scene {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  opacity: .03;
  background-image: radial-gradient(rgba(255,255,255,.3) 1px, transparent 1px);
  background-size: 24px 24px;
}

/* ═══════════════════════════════════════
   Card — dark translucent
   ═══════════════════════════════════════ */
.login-card {
  width: 420px;
  background: rgba(255, 255, 255, .03);
  border: 1px solid rgba(255, 255, 255, .06);
  border-radius: var(--radius-xl);
  overflow: hidden;
  position: relative;
  z-index: 1;

  &__brand {
    padding: 40px 40px 0;
    text-align: center;

    h2 {
      font-size: 22px;
      font-weight: var(--font-weight-bold);
      color: #fff;
      margin-top: 16px;
      letter-spacing: 0;
    }

    p {
      font-size: var(--font-size-s);
      color: rgba(255, 255, 255, .3);
      margin-top: 4px;
      letter-spacing: 1px;
      font-weight: var(--font-weight-strong);
    }
  }

  &__logo {
    display: inline-flex;
    opacity: .9;
  }

  &__form {
    padding: 36px 40px 40px;

    h3 {
      font-size: 16px;
      font-weight: var(--font-weight-strong);
      margin-bottom: 28px;
      color: rgba(255, 255, 255, .6);
      letter-spacing: 0;
    }
  }
}

/* ── Form overrides ── */
:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, .45) !important;
  font-size: var(--font-size-s);
  font-weight: var(--font-weight-strong);
  padding-bottom: 8px;
  letter-spacing: 0;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, .05) !important;
  border: 1px solid rgba(255, 255, 255, .08) !important;
  border-radius: var(--radius-sm) !important;
  box-shadow: none !important;
  transition: all var(--duration) var(--ease-in-out);

  &:hover {
    border-color: rgba(255, 255, 255, .15) !important;
    background: rgba(255, 255, 255, .07) !important;
  }
}

:deep(.el-input.is-focus .el-input__wrapper) {
  border-color: rgba(57, 100, 254, .5) !important;
  box-shadow: 0 0 0 4px rgba(57, 100, 254, .12) !important;
  background: rgba(255, 255, 255, .08) !important;
}

:deep(.el-input__inner) {
  color: #fff !important;
  font-weight: var(--font-weight-strong);

  &::placeholder {
    color: rgba(255, 255, 255, .18) !important;
  }
}

:deep(.el-input__prefix) {
  color: rgba(255, 255, 255, .2);
}

/* ── Login button ── */
.login-btn {
  width: 100% !important;
  height: 44px !important;
  font-size: var(--font-size-l) !important;
  font-weight: var(--font-weight-strong) !important;
  letter-spacing: 4px;
  border-radius: var(--radius-md) !important;
  background: var(--brand-primary) !important;
  border: none !important;
  transition: background-color var(--duration) var(--ease-in-out) !important;
  margin-top: 8px;

  &:hover {
    background: var(--brand-primary-hover) !important;
  }
}
</style>
