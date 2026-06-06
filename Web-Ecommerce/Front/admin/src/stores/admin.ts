import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ADMIN_TOKEN_KEY } from '@shared/constants'
import { adminLogin } from '@/api/admin'

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem(ADMIN_TOKEN_KEY) || '')
  const username = ref('')
  const role = ref<string>('')

  const isLoggedIn = computed(() => !!token.value)

  function hasRole(requiredRole: string): boolean {
    return role.value === requiredRole
  }

  async function login(credentials: { username: string; password: string }) {
    const res = await adminLogin(credentials)
    token.value = res.data.token
    username.value = credentials.username
    // JWT payload: { userId, role, iat, exp } — role 字段由后端签发
    try {
      const payload = JSON.parse(atob(res.data.token.split('.')[1]))
      role.value = payload.role || 'ADMIN'
    } catch {
      role.value = 'ADMIN'
    }
    localStorage.setItem(ADMIN_TOKEN_KEY, res.data.token)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem(ADMIN_TOKEN_KEY)
  }

  return { token, username, role, isLoggedIn, hasRole, login, logout }
})
