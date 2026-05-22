import type { AxiosError } from 'axios'
import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'

export function handleResponseError(
  error: AxiosError<{ message?: string }>,
  tokenKey: string,
  router: Router,
  loginPath = '/login',
): Promise<never> {
  if (error.response) {
    const { status, data } = error.response
    if (status === 401) {
      ElMessage.error(data?.message || '请先登录')
      localStorage.removeItem(tokenKey)
      router.push(loginPath)
      return Promise.reject(error)
    }
    ElMessage.error(data?.message || error.message || '请求失败')
  } else {
    ElMessage.error('网络异常，请稍后重试')
  }
  return Promise.reject(error)
}
