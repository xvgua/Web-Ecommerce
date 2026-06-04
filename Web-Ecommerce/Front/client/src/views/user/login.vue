<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__brand">
        <router-link to="/" class="auth-card__logo">&#x1F6D2; 电商平台</router-link>
        <div class="auth-card__illustration">
          <svg viewBox="0 0 300 300" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="auth-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#667eea" />
                <stop offset="100%" style="stop-color:#764ba2" />
              </linearGradient>
            </defs>
            <circle cx="150" cy="150" r="130" fill="url(#auth-grad)" opacity="0.12" />
            <circle cx="150" cy="150" r="100" fill="url(#auth-grad)" opacity="0.2" />
            <circle cx="150" cy="150" r="65" fill="url(#auth-grad)" opacity="0.35" />
            <text x="150" y="145" text-anchor="middle" font-size="56" fill="#fff" font-family="system-ui">&#x1F6E1;</text>
            <text x="150" y="210" text-anchor="middle" font-size="14" fill="#667eea" font-weight="600" font-family="system-ui">安全 · 便捷 · 信赖</text>
          </svg>
        </div>
        <p class="auth-card__slogan">海量好物，尽在掌握</p>
      </div>

      <div class="auth-card__form">
        <h1>欢迎回来</h1>
        <p class="auth-card__sub">登录您的账户继续购物</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @submit.prevent="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
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
            <div class="auth-form__extra">
              <el-checkbox v-model="form.remember">记住密码</el-checkbox>
              <router-link to="/forgot-password">忘记密码？</router-link>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" native-type="submit" class="auth-form__submit">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-card__switch">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { usernameRules, passwordRules } from '@shared/validators'
import { REMEMBERED_USERNAME } from '@shared/constants'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules: FormRules = {
  username: usernameRules,
  password: passwordRules,
}

onMounted(() => {
  const remembered = localStorage.getItem(REMEMBERED_USERNAME)
  if (remembered) {
    form.username = remembered
    form.remember = true
  }
})

async function handleLogin() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password, remember: form.remember })
    if (form.remember) {
      localStorage.setItem(REMEMBERED_USERNAME, form.username)
    } else {
      localStorage.removeItem(REMEMBERED_USERNAME)
    }
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding: 24px;
}

.auth-card {
  display: flex;
  max-width: 880px;
  width: 100%;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0,0,0,.1);
  overflow: hidden;

  &__brand {
    flex: 1;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 48px 40px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
  }

  &__logo {
    font-size: 22px;
    font-weight: 700;
    color: #fff;
    margin-bottom: 32px;
  }

  &__illustration {
    width: 220px;
    height: 220px;
    margin-bottom: 24px;
  }

  &__slogan {
    color: rgba(255,255,255,.85);
    font-size: 15px;
  }

  &__form {
    flex: 1;
    padding: 48px 48px;
    display: flex;
    flex-direction: column;
    justify-content: center;

    h1 {
      font-size: 26px;
      font-weight: 700;
      margin-bottom: 6px;
    }
  }

  &__sub {
    color: #909399;
    font-size: 14px;
    margin-bottom: 32px;
  }

  &__switch {
    text-align: center;
    font-size: 14px;
    color: #909399;
    margin-top: 8px;

    a {
      color: #409eff;
      font-weight: 500;
    }
  }
}

.auth-form {
  &__extra {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;

    a {
      color: #409eff;
      font-size: 13px;
    }
  }

  &__submit {
    width: 100%;
    height: 44px;
    font-size: 16px;
    letter-spacing: 4px;
    border-radius: 8px;
  }
}

@media (max-width: 768px) {
  .auth-card {
    flex-direction: column;
    max-width: 420px;

    &__brand {
      padding: 32px 24px;
    }

    &__illustration {
      width: 140px;
      height: 140px;
    }

    &__logo {
      font-size: 18px;
      margin-bottom: 20px;
    }

    &__form {
      padding: 32px 28px;

      h1 { font-size: 22px; }
    }
  }
}
</style>
