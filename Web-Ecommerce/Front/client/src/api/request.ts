import axios from 'axios'
import { ElMessage } from 'element-plus'
import { TOKEN_KEY } from '@shared/constants'
import { handleResponseError } from '@shared/utils/handleResponseError'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      const skipToast = (response.config as Record<string, unknown>)._skipErrorToast
      if (!skipToast) {
        ElMessage.error(res.message || '请求失败')
      }
      if (res.code === 401) {
        localStorage.removeItem(TOKEN_KEY)
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const skipToast = (error.config as Record<string, unknown>)?._skipErrorToast
    if (!skipToast) {
      return handleResponseError(error, TOKEN_KEY, router)
    }
    return Promise.reject(error)
  },
)

export default request
