import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@shared/types/user'
import { TOKEN_KEY } from '@shared/constants'
import { login as loginApi, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<User | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || 'USER')

  function setToken(val: string) {
    token.value = val
    localStorage.setItem(TOKEN_KEY, val)
  }

  function setUser(val: User) {
    user.value = val
  }

  function hasRole(requiredRole: string): boolean {
    return role.value === requiredRole
  }

  async function login(credentials: { username: string; password: string }) {
    const res = await loginApi(credentials)
    setToken(res.data.token)
    setUser(res.data.user)
  }

  async function fetchUser() {
    const res = await getUserInfo()
    setUser(res.data)
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, user, isLoggedIn, role, hasRole, login, fetchUser, logout, setToken, setUser }
})
