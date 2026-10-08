import { api } from './api'

export const authService = {
  requestOtp: (email: string, storeName?: string) =>
    api.post('/auth/request-otp', { email, storeName }),

  verifyOtp: (email: string, otp: string) =>
    api.post('/auth/verify-otp', { email, otp }).then((r) => r.data.data),

  me: () => api.get('/auth/me').then((r) => r.data.data),

  logout: () => api.post('/auth/logout')
}
