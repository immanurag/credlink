import axios from 'axios'

// LLM keys, Twilio tokens, JWT secret, and DB credentials all live on the backend only -
// the frontend only ever holds the short-lived access/refresh tokens issued after OTP login.
const envBaseUrl = import.meta.env.VITE_API_URL
const apiBaseUrl = envBaseUrl ? envBaseUrl.replace(/\/+$/, '') : '/api/v1'

export const api = axios.create({
  baseURL: apiBaseUrl
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('credlink_access_token')
  if (token && token !== 'null' && token !== 'undefined') {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRefreshing = false
let pendingRequests: Array<() => void> = []

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const url = original?.url || ''
    if ((status === 401 || status === 403) && !original._retry && !url.includes('/auth/')) {
      original._retry = true
      const refreshToken = localStorage.getItem('credlink_refresh_token')
      if (!refreshToken || refreshToken === 'null' || refreshToken === 'undefined') {
        clearSession()
        return Promise.reject(error)
      }
      if (!isRefreshing) {
        isRefreshing = true
        try {
          const { data } = await api.post('/auth/refresh', { refreshToken })
          storeSession(data.data)
          isRefreshing = false
          pendingRequests.forEach((cb) => cb())
          pendingRequests = []
        } catch {
          isRefreshing = false
          clearSession()
          return Promise.reject(error)
        }
      }
      return new Promise((resolve) => {
        pendingRequests.push(() => resolve(api(original)))
      })
    }
    return Promise.reject(error)
  }
)

export function storeSession(tokens: { accessToken: string; refreshToken: string; merchantId: number; storeName: string; email: string }) {
  localStorage.setItem('credlink_access_token', tokens.accessToken)
  localStorage.setItem('credlink_refresh_token', tokens.refreshToken)
  localStorage.setItem('credlink_merchant', JSON.stringify({ id: tokens.merchantId, storeName: tokens.storeName, email: tokens.email }))
}

export function clearSession() {
  localStorage.removeItem('credlink_access_token')
  localStorage.removeItem('credlink_refresh_token')
  localStorage.removeItem('credlink_merchant')
  if (window.location.pathname !== '/auth/login') {
    window.location.href = '/auth/login?sessionExpired=true'
  }
}

export function getStoredMerchant() {
  const token = localStorage.getItem('credlink_access_token')
  const raw = localStorage.getItem('credlink_merchant')
  if (!token || !raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function extractApiError(err: any): string {
  return (
    err?.response?.data?.error?.message ||
    err?.response?.data?.message ||
    (typeof err?.response?.data === 'string' && err.response.data.trim() ? err.response.data : null) ||
    err?.message ||
    'Something went wrong. Please try again.'
  )
}
