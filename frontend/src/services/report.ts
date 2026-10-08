import { api } from './api'

export const reportService = {
  statement: (customerId: number, from?: string, to?: string) =>
    api.get(`/reports/customers/${customerId}/statement`, { params: { from, to } }).then((r) => r.data.data),

  exportUrl: (customerId: number, format: 'pdf' | 'excel') =>
    `/api/v1/reports/customers/${customerId}/statement/export?format=${format}`
}
