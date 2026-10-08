export interface Customer {
  id: number
  name: string
  phone?: string
  address?: string
  category?: string
  trustScore?: number
  creditLimit?: number
  currentBalance: number
  createdAt: string
}

export type TransactionType = 'UDHAAR' | 'JAMA'
export type TransactionStatus = 'PENDING_REVIEW' | 'CONFIRMED' | 'DISCARDED'
export type TransactionSource = 'VOICE' | 'MANUAL' | 'UPI'

export interface Transaction {
  id: number
  customerId: number
  type: TransactionType
  amount: number
  balanceAfter: number
  description?: string
  source: TransactionSource
  voiceTranscript?: string
  confidenceScore?: number
  status: TransactionStatus
  createdAt: string
  confirmedAt?: string
}

export interface NlpExtractionResult {
  customerName: string
  amount?: number
  type?: TransactionType
  outstandingAmount?: number
  confidence: number
  valid: boolean
  rejectionReason?: string
  englishDescription?: string
  intent?: 'TRANSACTION' | 'OUTSTANDING_BALANCE_QUERY'
  customerFound?: boolean
  responseMessage?: string
}

export type VoiceState = 'IDLE' | 'LISTENING' | 'TRANSCRIBING' | 'EXTRACTING' | 'REVIEW' | 'CONFIRMED' | 'FAILED'

export interface DashboardSummary {
  totalLenaHai: number
  totalDenaHai: number
  todaysJama: number
  todaysUdhaar: number
  todaysTransactionCount: number
  activeCustomerCount: number
  newCustomersThisWeek: number
}

export interface TrendPoint {
  date: string
  udhaar: number
  jama: number
}

export interface Merchant {
  id: number
  email: string
  storeName: string
  ownerName?: string
  phone?: string
  address?: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  merchantId: number
  storeName: string
  email: string
}
