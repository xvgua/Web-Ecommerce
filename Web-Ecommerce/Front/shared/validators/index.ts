import type { FormItemRule } from 'element-plus'

export const requiredRule = (label: string): FormItemRule => ({
  required: true,
  message: `${label}不能为空`,
  trigger: 'blur',
})

export const lengthRule = (min: number, max: number, label: string): FormItemRule => ({
  min,
  max,
  message: `${label}长度应在 ${min}-${max} 个字符之间`,
  trigger: 'blur',
})

export const usernameRules: FormItemRule[] = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 3, max: 20, message: '用户名长度应在 3-20 个字符之间', trigger: 'blur' },
  { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
]

export const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, max: 20, message: '密码长度应在 6-20 个字符之间', trigger: 'blur' },
]

export const emailRules: FormItemRule[] = [
  { required: true, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
]

export const phoneRules: FormItemRule[] = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
]

export const captchaRules: FormItemRule[] = [
  { required: true, message: '请输入验证码', trigger: 'blur' },
  { len: 6, message: '验证码为 6 位数字', trigger: 'blur' },
  { pattern: /^\d{6}$/, message: '验证码格式不正确', trigger: 'blur' },
]

export const priceRules: FormItemRule[] = [
  { required: true, message: '请输入价格', trigger: 'blur' },
  {
    pattern: /^\d+(\.\d{1,2})?$/,
    message: '请输入正确的价格（最多两位小数）',
    trigger: 'blur',
  },
]
