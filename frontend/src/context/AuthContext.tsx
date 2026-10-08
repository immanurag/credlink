import React, { createContext, useContext, useEffect, useState } from 'react'
import { getStoredMerchant, storeSession, clearSession } from '../services/api'
import { authService } from '../services/auth'

interface MerchantSession {
  id: number
  storeName: string
  email: string
}

interface AuthContextValue {
  merchant: MerchantSession | null
  isAuthenticated: boolean
  loading: boolean
  requestOtp: (email: string, storeName?: string) => Promise<void>
  verifyOtp: (email: string, otp: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [merchant, setMerchant] = useState<MerchantSession | null>(getStoredMerchant())
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setMerchant(getStoredMerchant())
  }, [])

  const requestOtp = async (email: string, storeName?: string) => {
    setLoading(true)
    try {
      await authService.requestOtp(email, storeName)
    } finally {
      setLoading(false)
    }
  }

  const verifyOtp = async (email: string, otp: string) => {
    setLoading(true)
    try {
      const tokens = await authService.verifyOtp(email, otp)
      storeSession(tokens)
      setMerchant({ id: tokens.merchantId, storeName: tokens.storeName, email: tokens.email })
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    authService.logout().catch(() => {})
    clearSession()
    setMerchant(null)
  }

  return (
    <AuthContext.Provider value={{ merchant, isAuthenticated: !!merchant, loading, requestOtp, verifyOtp, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
