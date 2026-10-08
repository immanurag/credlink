import { api } from './api'
import type { Customer, Transaction } from '../types'

export const customerService = {
  list: (search?: string) =>
    api.get<{ data: Customer[] }>('/customers', { params: { search } }).then((r) => r.data.data),

  get: (id: number) => api.get<{ data: Customer }>(`/customers/${id}`).then((r) => r.data.data),

  create: (payload: Partial<Customer>) =>
    api.post<{ data: Customer }>('/customers', payload).then((r) => r.data.data),

  update: (id: number, payload: Partial<Customer>) =>
    api.put<{ data: Customer }>(`/customers/${id}`, payload).then((r) => r.data.data),

  remove: (id: number) => api.delete(`/customers/${id}`),

  transactions: (id: number) =>
    api.get<{ data: Transaction[] }>(`/customers/${id}/transactions`).then((r) => r.data.data)
}
