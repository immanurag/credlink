import { api } from './api'
import type { DashboardSummary, TrendPoint } from '../types'

export const dashboardService = {
  summary: () => api.get<{ data: DashboardSummary }>('/dashboard/summary').then((r) => r.data.data),
  trend: (days = 7) => api.get<{ data: TrendPoint[] }>('/dashboard/trend', { params: { days } }).then((r) => r.data.data)
}
