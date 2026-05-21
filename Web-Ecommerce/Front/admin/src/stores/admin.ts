import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ADMIN_TOKEN_KEY } from '@shared/constants'
import { adminLogin } from '@/api/admin'

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem(ADMIN_TOKEN_KEY) || '')
  const username = ref('')

  const isLoggedIn = computed(() => !!token.value)

  async function login(credentials: { username: string; password: string }) {
    const res = await adminLogin(credentials)
    token.value = res.data.token
    username.value = credentials.username
    localStorage.setItem(ADMIN_TOKEN_KEY, res.data.token)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem(ADMIN_TOKEN_KEY)
  }

  return { token, username, isLoggedIn, login, logout }
})
