import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 120000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const ct = String(res.headers['content-type'] || '')
    if (!ct.includes('application/json')) {
      return res
    }
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 200) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message))
      }
      return { ...res, data: body.data }
    }
    return res
  },
  (err) => {
    const status = err.response?.status
    const reqUrl = String(err.config?.url || '')
    // 令牌失效或后端拒绝匿名访问时多为 401/403；路由仍认为已登录，需清会话并回到登录页
    if ((status === 401 || status === 403) && !reqUrl.includes('/api/auth/')) {
      const auth = useAuthStore()
      if (auth.token) {
        auth.logout()
        if (!window.location.pathname.startsWith('/login')) {
          window.location.assign('/login')
        }
        return Promise.reject(err)
      }
    }
    const d = err.response?.data
    const msg =
      (typeof d === 'object' && d && 'message' in d && (d as { message?: string }).message) ||
      err.message ||
      '网络错误'
    ElMessage.error(String(msg))
    return Promise.reject(err)
  }
)

export default http
