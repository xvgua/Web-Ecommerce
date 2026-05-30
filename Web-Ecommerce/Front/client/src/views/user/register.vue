<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__brand">
        <router-link to="/" class="auth-card__logo">&#x1F6D2; 电商平台</router-link>
        <div class="auth-card__illustration">
          <svg viewBox="0 0 300 300" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="reg-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#43e97b" />
                <stop offset="100%" style="stop-color:#38f9d7" />
              </linearGradient>
            </defs>
            <circle cx="150" cy="150" r="130" fill="url(#reg-grad)" opacity="0.12" />
            <circle cx="150" cy="150" r="100" fill="url(#reg-grad)" opacity="0.2" />
            <circle cx="150" cy="150" r="65" fill="url(#reg-grad)" opacity="0.35" />
            <text x="150" y="145" text-anchor="middle" font-size="56" fill="#fff" font-family="system-ui">&#x1F31F;</text>
            <text x="150" y="210" text-anchor="middle" font-size="14" fill="#43e97b" font-weight="600" font-family="system-ui">加入我们 探索无限可能</text>
          </svg>
        </div>
        <p class="auth-card__slogan">注册即享专属优惠</p>
      </div>

      <div class="auth-card__form">
        <h1>创建账号</h1>
        <p class="auth-card__sub">立即注册开始您的购物之旅</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @submit.prevent="handleRegister">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <div class="email-row">
              <el-input v-model="form.email" placeholder="请输入邮箱" size="large" :prefix-icon="Message" class="email-row__input" />
              <el-button size="large" :disabled="codeCooldown > 0" :loading="sendingCode" @click="sendCode" class="email-row__btn">
                {{ codeCooldown > 0 ? `${codeCooldown}s 后重发` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="验证码" prop="captcha">
            <el-input v-model="form.captcha" placeholder="请输入 6 位验证码" size="large" maxlength="6" :prefix-icon="Key" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" native-type="submit" class="auth-form__submit">
              注 册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-card__switch">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { register, sendRegisterCode } from '@/api/user'
import { usernameRules, passwordRules, emailRules, captchaRules } from '@shared/validators'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

async function sendCode() {
  // Pre-validate email format before sending
  try {
    await formRef.value?.validateField('email')
  } catch {
    return
  }
  sendingCode.value = true
  try {
    await sendRegisterCode(form.email)
    ElMessage.success('验证码已发送至您的邮箱')
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) {
        clearInterval(cooldownTimer!)
        cooldownTimer = null
      }
    }, 1000)
  } catch { /* handled by interceptor */ }
  finally { sendingCode.value = false }
}

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: '',
})

const validateConfirmPass = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: usernameRules,
  email: emailRules,
  captcha: captchaRules,
  password: passwordRules,
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPass, trigger: 'blur' },
  ],
}

// Reset cooldown and captcha when user changes email (code is bound to old email)
watch(() => form.email, () => {
  if (codeCooldown.value > 0) {
    codeCooldown.value = 0
    if (cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }
  form.captcha = ''
})

async function handleRegister() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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
    text-decoration: none;
  }

  &__illustration {
    width: 200px;
    height: 200px;
    margin-bottom: 24px;
  }

  &__slogan {
    color: rgba(255,255,255,.85);
    font-size: 15px;
  }

  &__form {
    flex: 1;
    padding: 40px 48px;
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
    margin-bottom: 28px;
  }

  &__switch {
    text-align: center;
    font-size: 14px;
    color: #909399;
    margin-top: 4px;

    a {
      color: #409eff;
      font-weight: 500;
    }
  }
}

.email-row {
  display: flex;
  gap: 10px;

  &__input {
    flex: 1;
  }

  &__btn {
    flex-shrink: 0;
    white-space: nowrap;
  }
}

.auth-form {
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
      width: 120px;
      height: 120px;
    }

    &__logo {
      font-size: 18px;
      margin-bottom: 16px;
    }

    &__form {
      padding: 32px 28px;

      h1 { font-size: 22px; }
    }
  }
}
</style>
