import { api } from './api'

export const notificationService = {
  list: () => api.get('/notifications').then((r) => r.data.data),
  remindOne: (customerId: number) => api.post(`/notifications/customers/${customerId}/remind`),
  remindAllOverdue: () => api.post('/notifications/customers/remind-overdue').then((r) => r.data.data)
}
