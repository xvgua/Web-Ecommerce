<template>
  <div class="admin-login">
    <!-- Organic floating blobs background -->
    <div class="login-scene">
      <div class="login-blob login-blob--1" />
      <div class="login-blob login-blob--2" />
      <div class="login-blob login-blob--3" />
      <div class="login-blob login-blob--4" />
    </div>

    <div class="login-card">
      <div class="login-card__brand">
        <div class="login-card__logo">
          <svg viewBox="0 0 52 52" width="52" height="52" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="login-logo-grad" x1="6" y1="6" x2="46" y2="46">
                <stop offset="0%" stop-color="#6eb89a" />
                <stop offset="100%" stop-color="#8cc9aa" />
              </linearGradient>
            </defs>
            <rect x="6" y="6" width="40" height="40" rx="16" fill="url(#login-logo-grad)" />
            <path d="M26 17c-4 4-7 9-7 9h5v7h3v-7h5s-2-5-6-9z" fill="#fff" opacity=".95" />
            <circle cx="26" cy="22" r="3" fill="#fff" opacity=".5" />
          </svg>
        </div>
        <h2>管理后台</h2>
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
  background: #1a241c;
  position: relative;
  padding: 24px;
  overflow: hidden;
}

/* ═══════════════════════════════════════
   Organic blob scene
   ═══════════════════════════════════════ */
.login-scene {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.login-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: .08;

  &--1 {
    width: 500px; height: 500px;
    background: #6eb89a;
    top: -120px; right: -80px;
    animation: blob-float-a 18s ease-in-out infinite;
  }

  &--2 {
    width: 400px; height: 400px;
    background: #b8b0e0;
    bottom: -100px; left: -60px;
    animation: blob-float-b 22s ease-in-out infinite;
  }

  &--3 {
    width: 300px; height: 300px;
    background: #e8a860;
    top: 40%; left: 40%;
    animation: blob-float-c 20s ease-in-out infinite;
  }

  &--4 {
    width: 250px; height: 250px;
    background: #8ab8d8;
    top: 10%; left: 10%;
    animation: blob-float-d 24s ease-in-out infinite;
  }
}

@keyframes blob-float-a {
  0%, 100% { transform: translate(0, 0) scale(1) rotate(0deg); }
  33% { transform: translate(60px, -40px) scale(1.08) rotate(5deg); }
  66% { transform: translate(-30px, 30px) scale(.94) rotate(-3deg); }
}

@keyframes blob-float-b {
  0%, 100% { transform: translate(0, 0) scale(1) rotate(0deg); }
  33% { transform: translate(-50px, -30px) scale(1.06) rotate(-4deg); }
  66% { transform: translate(40px, 20px) scale(.93) rotate(3deg); }
}

@keyframes blob-float-c {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-40px, -50px) scale(1.1); }
}

@keyframes blob-float-d {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(50px, -30px) scale(1.12); }
}

/* ═══════════════════════════════════════
   Glass card
   ═══════════════════════════════════════ */
.login-card {
  width: 460px;
  background: rgba(255, 255, 255, .04);
  backdrop-filter: blur(28px) saturate(140%);
  -webkit-backdrop-filter: blur(28px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: var(--org-radius-xl);
  overflow: hidden;
  position: relative;
  z-index: 1;
  box-shadow: 0 24px 80px rgba(0, 0, 0, .3);

  &__brand {
    padding: 44px 44px 0;
    text-align: center;

    h2 {
      font-size: 23px;
      font-weight: 700;
      color: #fff;
      margin-top: 18px;
      letter-spacing: -.4px;
    }

    p {
      font-size: 13px;
      color: rgba(255, 255, 255, .3);
      margin-top: 6px;
      letter-spacing: 1.5px;
      text-transform: uppercase;
      font-weight: 600;
    }
  }

  &__logo {
    display: inline-flex;
    filter: drop-shadow(0 4px 16px rgba(110, 184, 154, .4));
  }

  &__form {
    padding: 40px 44px 44px;

    h3 {
      font-size: 17px;
      font-weight: 700;
      margin-bottom: 32px;
      color: rgba(255, 255, 255, .8);
      letter-spacing: -.2px;
    }
  }
}

/* ── Form overrides ── */
:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, .5) !important;
  font-size: 13px;
  font-weight: 600;
  padding-bottom: 8px;
  letter-spacing: .3px;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, .05) !important;
  border: 1px solid rgba(255, 255, 255, .1) !important;
  border-radius: var(--org-radius-sm) !important;
  box-shadow: none !important;
  transition: all var(--org-duration) var(--org-ease);

  &:hover {
    border-color: rgba(255, 255, 255, .2) !important;
    background: rgba(255, 255, 255, .07) !important;
  }
}

:deep(.el-input.is-focus .el-input__wrapper) {
  border-color: rgba(110, 184, 154, .5) !important;
  box-shadow: 0 0 0 4px rgba(110, 184, 154, .12) !important;
  background: rgba(255, 255, 255, .08) !important;
}

:deep(.el-input__inner) {
  color: #fff !important;
  font-weight: 500;

  &::placeholder {
    color: rgba(255, 255, 255, .2) !important;
  }
}

:deep(.el-input__prefix) {
  color: rgba(255, 255, 255, .25);
}

/* ── Login button ── */
.login-btn {
  width: 100% !important;
  height: 50px !important;
  font-size: 16px !important;
  font-weight: 700 !important;
  letter-spacing: 5px;
  border-radius: var(--org-radius-full) !important;
  background: linear-gradient(135deg, #6eb89a, #5aad8a) !important;
  border: none !important;
  transition: all var(--org-duration) var(--org-ease) !important;
  margin-top: 8px;

  &:hover {
    background: linear-gradient(135deg, #5aad8a, #4a9b7a) !important;
    box-shadow: 0 8px 32px rgba(110, 184, 154, .4) !important;
    transform: translateY(-2px) !important;
  }

  &:active {
    transform: translateY(0) scale(.97) !important;
  }
}
</style>
