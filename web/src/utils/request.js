import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearAuthSession, readStoredToken } from '@/utils/session'

// 创建独立的 axios 实例，便于统一配置
const request = axios.create({
  baseURL: '/api',
  // 后端 AI 请求默认超时 30000ms，这里给前端留更大余量
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ---- 请求拦截器：自动注入 JWT Token ----
request.interceptors.request.use(
  (config) => {
    const token = readStoredToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---- 响应拦截器：统一处理业务错误码 ----
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回格式 { code, message, data }
    // code 为 0 或 200 表示成功
    if (res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message

    if (status === 401) {
      // Token 失效或未登录：清除本地状态并跳转到登录页
      clearAuthSession()
      ElMessage.warning('登录已过期，请重新登录')
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if (status === 403) {
      ElMessage.error('权限不足')
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (status === 500) {
      ElMessage.error('服务器内部错误，请稍后重试')
    } else {
      ElMessage.error(msg || error.message || '网络请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
