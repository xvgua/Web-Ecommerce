export interface User {
  id: number
  accountId: number
  username: string
  email: string
  nickname: string
  avatar: string
  phone: string
  gender: number
  intro: string
  usernameUpdateTime: string
  status: number
  createTime: string
}

export interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  email: string
  captcha: string
}

export interface ResetPasswordForm {
  email: string
  code: string
  newPassword: string
}

export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
}

export interface Address {
  id: number
  userId: number
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}

export interface AddressForm {
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}

export interface Admin {
  id: number
  username: string
  role: string
  status: number
  createTime: string
}
