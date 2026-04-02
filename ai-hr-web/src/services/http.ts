import axios from 'axios'
import { STORAGE_KEY, useAuthStore } from '../stores/auth'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const http = axios.create({
  baseURL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.isAuthenticated) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  if (auth.tenantId) {
    config.headers['X-Tenant-Id'] = auth.tenantId
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as unknown
    const wrapped =
      payload &&
      typeof payload === 'object' &&
      !Array.isArray(payload) &&
      'code' in payload &&
      'message' in payload

    if (!wrapped) {
      return response
    }

    const envelope = payload as { code: string | number; message?: string; data?: unknown }
    const code = envelope.code
    const success = code === 0 || code === '0'

    if (success && 'data' in envelope) {
      response.data = envelope.data
      return response
    }

    const error = new Error(envelope.message || '请求失败') as Error & {
      response?: { data?: unknown; status?: number }
    }
    error.response = {
      data: payload,
      status: response.status
    }
    return Promise.reject(error)
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      localStorage.removeItem(STORAGE_KEY)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)
