import React, { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { extractApiError } from '../services/api'
import { KeyRound, ArrowRight, Loader2, ShieldCheck } from 'lucide-react'

export default function VerifyOtp() {
  const location = useLocation() as { state?: { email?: string } }
  const [email, setEmail] = useState(location.state?.email || '')
  const [otp, setOtp] = useState('')
  const [error, setError] = useState('')
  const { verifyOtp, loading } = useAuth()
  const navigate = useNavigate()

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await verifyOtp(email, otp)
      navigate('/dashboard')
    } catch (err) {
      setError(extractApiError(err))
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl border border-slate-200 shadow-xl p-8 space-y-6">
        <div className="text-center space-y-2">
          <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white font-display font-extrabold text-2xl flex items-center justify-center mx-auto shadow-lg shadow-indigo-600/20">
            <KeyRound className="w-7 h-7" />
          </div>
          <h1 className="font-display font-extrabold text-2xl text-slate-900">Verify Security OTP</h1>
          <p className="text-xs text-slate-500">
            {email ? (
              <>
                Enter the 6-digit verification code sent to <span className="font-bold text-slate-900">{email}</span>.
              </>
            ) : (
              'Enter the 6-digit verification code sent to your email.'
            )}
          </p>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-900 rounded-xl text-xs">
            {error}
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-4">
          {!email && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Email</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@store.com"
                className="input-field"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">6-Digit OTP Code</label>
            <input
              required
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              placeholder="123456"
              maxLength={6}
              className="input-field text-center font-display font-extrabold text-2xl tracking-[0.4em] py-3 text-indigo-700"
            />
          </div>

          <button type="submit" disabled={loading} className="btn-primary w-full py-3 text-base font-semibold">
            {loading ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Verifying OTP...
              </>
            ) : (
              <>
                Verify &amp; Open Dashboard
                <ArrowRight className="w-5 h-5" />
              </>
            )}
          </button>
        </form>

        <div className="text-center">
          <button
            onClick={() => navigate('/auth/login')}
            className="text-xs text-slate-500 hover:text-indigo-600 font-semibold"
          >
            &larr; Back to Email Entry
          </button>
        </div>
      </div>
    </div>
  )
}
