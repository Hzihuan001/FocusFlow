import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一使用相对路径 /api，通过 Nginx 代理到后端
// 这样 Cookie 可以正常传递，避免跨域问题
const baseURL = '/api'

const request = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: true  // 携带 Cookie（Session ID）
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401) {
      // 未授权：清除登录状态，跳转登录页
      localStorage.removeItem('adminUser')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(new Error('未授权'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    // 处理 HTTP 错误状态码
    const status = error.response?.status
    if (status === 401) {
      // 未授权：清除登录状态，跳转登录页
      localStorage.removeItem('adminUser')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('没有权限执行此操作')
    } else if (status === 500) {
      ElMessage.error('服务器内部错误，请稍后重试')
    } else if (status === 502 || status === 503) {
      ElMessage.error('服务暂时不可用，请稍后重试')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
