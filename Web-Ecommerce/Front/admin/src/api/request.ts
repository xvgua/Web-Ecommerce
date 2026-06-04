import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ADMIN_TOKEN_KEY } from '@shared/constants'
import { handleResponseError } from '@shared/utils/handleResponseError'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    // Skip JSON parsing for blob responses (e.g., file downloads)
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem(ADMIN_TOKEN_KEY)
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => handleResponseError(error, ADMIN_TOKEN_KEY, router),
)

export default request
