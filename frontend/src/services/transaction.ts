import { api } from './api'
import type { Transaction, TransactionType, TransactionSource } from '../types'

export interface PreviewPayload {
  customerId: number
  type: TransactionType
  amount: number
  description?: string
  source?: TransactionSource
  voiceTranscript?: string
  confidenceScore?: number
  explicitOutstanding?: number
}

export const transactionService = {
  preview: (payload: PreviewPayload) =>
    api.post<{ data: Transaction }>('/transactions/preview', payload).then((r) => r.data.data),

  confirm: (id: number, edits?: Partial<PreviewPayload>) =>
    api.post<{ data: Transaction }>(`/transactions/${id}/confirm`, edits || {}).then((r) => r.data.data),

  discard: (id: number) => api.delete(`/transactions/${id}`),

  get: (id: number) => api.get<{ data: Transaction }>(`/transactions/${id}`).then((r) => r.data.data)
}
