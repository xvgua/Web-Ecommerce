<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__form" style="flex:1;max-width:440px">
        <h1>找回密码</h1>
        <p class="auth-card__sub">{{ step === 1 ? '请输入注册邮箱' : '设置新密码' }}</p>

        <!-- Step 1: Enter email -->
        <el-form v-if="step === 1" ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top" class="auth-form" @submit.prevent="handleSendCode">
          <el-form-item label="注册邮箱" prop="email">
            <el-input v-model="emailForm.email" placeholder="请输入注册时使用的邮箱" size="large" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="sending" native-type="submit" class="auth-form__submit">
              {{ sending ? `${countdown}s 后重新发送` : '获取验证码' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- Step 2: Enter code + new password -->
        <el-form v-else ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top" class="auth-form" @submit.prevent="handleReset">
          <el-form-item label="验证码" prop="code">
            <el-input v-model="resetForm.code" placeholder="请输入6位验证码" size="large" maxlength="6" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" native-type="submit" class="auth-form__submit">
              重置密码
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button text type="primary" @click="handleResend" :disabled="countdown > 0">
              {{ countdown > 0 ? `${countdown}s 后重新发送` : '重新发送验证码' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-card__switch">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api/user'
import { emailRules, captchaRules, passwordRules } from '@shared/validators'

const router = useRouter()
const step = ref(1)
const sending = ref(false)
const loading = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const emailFormRef = ref<FormInstance>()
const emailForm = reactive({ email: '' })

const resetFormRef = ref<FormInstance>()
const resetForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const emailOnlyRules: FormRules = { email: emailRules }

const resetRules: FormRules = {
  code: captchaRules,
  newPassword: passwordRules,
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      if (timer) { clearInterval(timer); timer = null }
    }
  }, 1000)
}

async function handleSendCode() {
  const valid = await emailFormRef.value?.validate()
  if (!valid) return
  sending.value = true
  try {
    await sendResetCode(emailForm.email)
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
    step.value = 2
  } finally {
    sending.value = false
  }
}

async function handleResend() {
  if (countdown.value > 0) return
  sending.value = true
  try {
    await sendResetCode(emailForm.email)
    ElMessage.success('验证码已重新发送')
    startCountdown()
  } finally {
    sending.value = false
  }
}

async function handleReset() {
  const valid = await resetFormRef.value?.validate()
  if (!valid) return
  loading.value = true
  try {
    await resetPassword({
      email: emailForm.email,
      code: resetForm.code,
      newPassword: resetForm.newPassword,
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
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
  background: var(--bg1);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--line-light);
  overflow: hidden;
  justify-content: center;

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
  &__submit {
    width: 100%;
    height: 44px;
    font-size: 16px;
    letter-spacing: 4px;
    border-radius: 8px;
  }
}

@media (max-width: 768px) {
  .auth-card__form {
    padding: 32px 28px;

    h1 { font-size: 22px; }
  }
}
</style>
